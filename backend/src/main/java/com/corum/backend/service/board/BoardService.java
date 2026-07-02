package com.corum.backend.service.board;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.board.Board;
import com.corum.backend.domain.board.BoardCategory;
import com.corum.backend.domain.board.BoardCategoryRepository;
import com.corum.backend.domain.board.BoardGroupPermission;
import com.corum.backend.domain.board.BoardGroupPermissionRepository;
import com.corum.backend.domain.board.BoardRepository;
import com.corum.backend.domain.group.MemberGroupRepository;
import com.corum.backend.domain.post.PostRepository;
import com.corum.backend.dto.board.BoardCreateRequest;
import com.corum.backend.dto.board.BoardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardGroupPermissionRepository boardGroupPermissionRepository;
    private final BoardCategoryRepository boardCategoryRepository;
    private final PostRepository postRepository;
    private final MemberGroupRepository memberGroupRepository;

    // ===== 게시판 목록 =====
    @Transactional(readOnly = true)
    public List<BoardResponse> getBoards() {
        return boardRepository.findByIsActiveTrueOrderByIdAsc().stream()
                .map(b -> new BoardResponse(b,
                        boardGroupPermissionRepository.findByBoardId(b.getId()),
                        categoriesWithCount(b.getId())))
                .collect(Collectors.toList());
    }

    // ===== 게시판 단건 =====
    @Transactional(readOnly = true)
    public BoardResponse getBoard(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("게시판을 찾을 수 없습니다."));
        List<BoardGroupPermission> permissions = boardGroupPermissionRepository.findByBoardId(id);
        return new BoardResponse(board, permissions, categoriesWithCount(id));
    }

    // 카테고리 목록 + 각 카테고리의 게시글 수 채우기
    private List<BoardCategory> categoriesWithCount(Long boardId) {
        List<BoardCategory> cats = boardCategoryRepository.findByBoardIdOrderBySortOrderAsc(boardId);
        cats.forEach(c -> c.setPostCount(postRepository.countByCategoryId(c.getId())));
        return cats;
    }

    // ===== 게시판 생성 =====
    @Transactional
    public BoardResponse createBoard(BoardCreateRequest request) {
        Board board = Board.builder()
                .menuId(request.getMenuId())
                .name(request.getName())
                .boardType(request.getBoardType())
                .useComment(request.getUseComment())
                .useLike(request.getUseLike())
                .useAnonymous(request.getUseAnonymous())
                .useNotice(request.getUseNotice())
                .noticeCountLimit(request.getNoticeCountLimit())
                .fileMaxSizeMb(request.getFileMaxSizeMb())
                .fileAllowedExtensions(request.getFileAllowedExtensions())
                .fileMaxCount(request.getFileMaxCount())
                .isActive(request.getIsActive())
                .useAllCategory(Boolean.TRUE.equals(request.getUseAllCategory()))
                .build();

        Board saved = boardRepository.save(board);
        savePermissions(saved.getId(), request.getPermissions());
        saveCategories(saved.getId(), request.getCategories());

        List<BoardGroupPermission> permissions = boardGroupPermissionRepository.findByBoardId(saved.getId());
        return new BoardResponse(saved, permissions, categoriesWithCount(saved.getId()));
    }

    // ===== 게시판 수정 =====
    @Transactional
    public BoardResponse updateBoard(Long id, BoardCreateRequest request) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("게시판을 찾을 수 없습니다."));
        board.update(request.getName(), request.getBoardType(), request.getUseComment(),
                request.getUseLike(), request.getUseAnonymous(), request.getUseNotice(),
                request.getNoticeCountLimit(), request.getFileMaxSizeMb(),
                request.getFileAllowedExtensions(), request.getFileMaxCount(),
                request.getIsActive(), request.getUseAllCategory());
        boardGroupPermissionRepository.deleteByBoardId(id);
        savePermissions(id, request.getPermissions());
        saveCategories(id, request.getCategories());
        List<BoardGroupPermission> permissions = boardGroupPermissionRepository.findByBoardId(id);
        return new BoardResponse(board, permissions, categoriesWithCount(id));
    }

    // ===== 게시판 삭제 =====
    @Transactional
    public void deleteBoard(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("게시판을 찾을 수 없습니다."));
        boardGroupPermissionRepository.deleteByBoardId(id);
        boardCategoryRepository.deleteByBoardId(id);
        boardRepository.delete(board);
    }

    // ===== 권한 확인 =====
    // 게시판에 board_group_permissions 행이 하나도 없으면 "공개 게시판"으로 간주한다
    // (calendar_group_permissions와 동일한 관례). WRITE만 로그인을 요구하고
    // READ/COMMENT/DOWNLOAD는 비로그인도 허용. 행이 존재하면 그 그룹 설정을 그대로 따른다.
    @Transactional(readOnly = true)
    public boolean hasPermission(Long boardId, Long memberId, String permType) {
        if (memberId != null && memberGroupRepository.existsSuperAdminGroupByMemberId(memberId)) {
            return true;
        }
        List<BoardGroupPermission> allPerms = boardGroupPermissionRepository.findByBoardId(boardId);
        if (allPerms.isEmpty()) {
            return !"WRITE".equals(permType) || memberId != null;
        }
        if (memberId == null) return false;
        List<Long> groupIds = memberGroupRepository.findGroupIdsByMemberId(memberId);
        if (groupIds.isEmpty()) return false;
        List<BoardGroupPermission> perms = boardGroupPermissionRepository
                .findByBoardIdAndGroupIds(boardId, groupIds);
        return perms.stream().anyMatch(p -> switch (permType) {
            case "READ"     -> Boolean.TRUE.equals(p.getCanRead());
            case "WRITE"    -> Boolean.TRUE.equals(p.getCanWrite());
            case "COMMENT"  -> Boolean.TRUE.equals(p.getCanComment());
            case "DOWNLOAD" -> Boolean.TRUE.equals(p.getCanDownload());
            default -> false;
        });
    }

    // ===== 내부 메서드 =====
    // 카테고리 업서트: 기존 id는 유지(게시글의 category_id 참조 보존),
    // 신규는 추가, 요청에서 빠진 기존 카테고리는 게시글이 없을 때만 삭제.
    private void saveCategories(Long boardId, List<BoardCreateRequest.CategoryRequest> reqs) {
        List<BoardCategory> existing = boardCategoryRepository.findByBoardIdOrderBySortOrderAsc(boardId);
        Map<Long, BoardCategory> existingById = existing.stream()
                .collect(Collectors.toMap(BoardCategory::getId, c -> c));

        List<Long> keepIds = (reqs == null) ? List.of()
                : reqs.stream()
                        .map(BoardCreateRequest.CategoryRequest::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

        // 삭제 대상: 기존에 있으나 요청 목록에서 빠진 카테고리
        for (BoardCategory cat : existing) {
            if (!keepIds.contains(cat.getId())) {
                long count = postRepository.countByCategoryId(cat.getId());
                if (count > 0) {
                    throw new BusinessException(
                            "'" + cat.getName() + "' 카테고리에 게시글이 " + count + "건 있어 삭제할 수 없습니다.");
                }
                boardCategoryRepository.delete(cat);
            }
        }

        if (reqs == null) return;
        int order = 0;
        for (BoardCreateRequest.CategoryRequest r : reqs) {
            if (r.getName() == null || r.getName().isBlank()) { order++; continue; }
            String name = r.getName().trim();
            if (r.getId() != null && existingById.containsKey(r.getId())) {
                existingById.get(r.getId()).update(name, order);   // 더티 체킹으로 반영
            } else {
                boardCategoryRepository.save(BoardCategory.builder()
                        .boardId(boardId)
                        .name(name)
                        .sortOrder(order)
                        .build());
            }
            order++;
        }
    }

    private void savePermissions(Long boardId, List<BoardCreateRequest.BoardPermissionRequest> reqs) {
        if (reqs == null || reqs.isEmpty()) return;
        List<BoardGroupPermission> permissions = reqs.stream()
                .filter(r -> r.getGroupId() != null)
                .map(r -> BoardGroupPermission.builder()
                        .boardId(boardId)
                        .groupId(r.getGroupId())
                        .canRead(r.getCanRead())
                        .canWrite(r.getCanWrite())
                        .canComment(r.getCanComment())
                        .canDownload(r.getCanDownload())
                        .canManage(Boolean.TRUE.equals(r.getCanManage()))
                        .build())
                .collect(Collectors.toList());
        boardGroupPermissionRepository.saveAll(permissions);
    }
}
