package com.corum.backend.service.board;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.admin.AdminMenu;
import com.corum.backend.domain.admin.AdminMenuGroupPermissionRepository;
import com.corum.backend.domain.admin.AdminMenuRepository;
import com.corum.backend.domain.board.Board;
import com.corum.backend.domain.board.BoardCategory;
import com.corum.backend.domain.board.BoardCategoryRepository;
import com.corum.backend.domain.board.BoardGroupPermission;
import com.corum.backend.domain.board.BoardGroupPermissionRepository;
import com.corum.backend.domain.board.BoardRepository;
import com.corum.backend.domain.board.BoardWriterIdentity;
import com.corum.backend.domain.board.BoardWriterIdentityRepository;
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

    private static final String BOARD_ADMIN_MENU_URL = "/admin/boards";

    private final BoardRepository boardRepository;
    private final BoardGroupPermissionRepository boardGroupPermissionRepository;
    private final BoardCategoryRepository boardCategoryRepository;
    private final BoardWriterIdentityRepository boardWriterIdentityRepository;
    private final PostRepository postRepository;
    private final MemberGroupRepository memberGroupRepository;
    private final AdminMenuRepository adminMenuRepository;
    private final AdminMenuGroupPermissionRepository adminMenuGroupPermissionRepository;

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
    public BoardResponse getBoard(Long id, Long memberId) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("게시판을 찾을 수 없습니다."));
        List<BoardGroupPermission> permissions = boardGroupPermissionRepository.findByBoardId(id);
        List<BoardWriterIdentity> identities = boardWriterIdentityRepository.findByBoardIdOrderBySortOrderAsc(id);
        return new BoardResponse(board, permissions, categoriesWithCount(id),
                identities, canUseAliasWriter(id, memberId));
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
                .useAliasWriter(Boolean.TRUE.equals(request.getUseAliasWriter()))
                .build();

        Board saved = boardRepository.save(board);
        savePermissions(saved.getId(), request.getPermissions());
        saveCategories(saved.getId(), request.getCategories());
        saveIdentities(saved.getId(), request.getWriterIdentities());

        List<BoardGroupPermission> permissions = boardGroupPermissionRepository.findByBoardId(saved.getId());
        List<BoardWriterIdentity> identities = boardWriterIdentityRepository.findByBoardIdOrderBySortOrderAsc(saved.getId());
        return new BoardResponse(saved, permissions, categoriesWithCount(saved.getId()), identities, false);
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
                request.getIsActive(), request.getUseAllCategory(), request.getUseAliasWriter());
        // 삭제와 재삽입을 같은 트랜잭션에서 하면 Hibernate가 삽입을 삭제보다 먼저
        // 플러시해서 (board_id, group_id) 유니크 제약에 걸리므로 즉시 flush
        boardGroupPermissionRepository.deleteByBoardId(id);
        boardGroupPermissionRepository.flush();
        savePermissions(id, request.getPermissions());
        saveCategories(id, request.getCategories());
        saveIdentities(id, request.getWriterIdentities());
        List<BoardGroupPermission> permissions = boardGroupPermissionRepository.findByBoardId(id);
        List<BoardWriterIdentity> identities = boardWriterIdentityRepository.findByBoardIdOrderBySortOrderAsc(id);
        return new BoardResponse(board, permissions, categoriesWithCount(id), identities, false);
    }

    // ===== 게시판 삭제 =====
    @Transactional
    public void deleteBoard(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("게시판을 찾을 수 없습니다."));
        boardGroupPermissionRepository.deleteByBoardId(id);
        boardCategoryRepository.deleteByBoardId(id);
        boardWriterIdentityRepository.deleteByBoardId(id);
        boardRepository.delete(board);
    }

    // ===== 권한 확인 =====
    // read/write/comment/download 네 유형을 서로 독립적으로 판단한다: 어떤 그룹에도
    // 해당 유형이 명시적으로 허용(true)된 행이 하나도 없으면 그 유형만 공개로 간주한다
    // (WRITE는 로그인만 되어 있으면 허용하는 기존 관례 유지). 예를 들어 "쓰기만 특정
    // 그룹으로 제한"하려고 그 그룹에 can_write=true 행 하나만 추가해도, 다른 그룹에
    // can_read=true인 행이 전혀 없다면 읽기는 계속 공개로 남는다 — 예전에는 게시판에
    // 행이 "하나라도" 있으면 read/write/comment/download 전부 그룹 화이트리스트로
    // 전환돼서 "읽기는 공개 + 쓰기만 제한" 조합이 불가능했던 문제를 해결한다.
    @Transactional(readOnly = true)
    public boolean hasPermission(Long boardId, Long memberId, String permType) {
        if (memberId != null && memberGroupRepository.existsSuperAdminGroupByMemberId(memberId)) {
            return true;
        }
        List<BoardGroupPermission> allPerms = boardGroupPermissionRepository.findByBoardId(boardId);

        List<Long> myGroupIds = memberId != null
                ? memberGroupRepository.findGroupIdsByMemberId(memberId)
                : List.of();
        List<BoardGroupPermission> myPerms = allPerms.stream()
                .filter(p -> myGroupIds.contains(p.getGroupId()))
                .toList();
        // 게시판 관리(can_manage) 권한이 있으면 READ/WRITE/COMMENT/DOWNLOAD 전부 허용
        if (myPerms.stream().anyMatch(BoardGroupPermission::getCanManage)) {
            return true;
        }

        boolean anyGroupGrantsThisType = allPerms.stream().anyMatch(p -> matchesPermType(p, permType));
        if (!anyGroupGrantsThisType) {
            return !"WRITE".equals(permType) || memberId != null;
        }

        if (memberId == null || myGroupIds.isEmpty()) return false;
        return myPerms.stream().anyMatch(p -> matchesPermType(p, permType));
    }

    private boolean matchesPermType(BoardGroupPermission p, String permType) {
        return switch (permType) {
            case "READ"     -> Boolean.TRUE.equals(p.getCanRead());
            case "WRITE"    -> Boolean.TRUE.equals(p.getCanWrite());
            case "COMMENT"  -> Boolean.TRUE.equals(p.getCanComment());
            case "DOWNLOAD" -> Boolean.TRUE.equals(p.getCanDownload());
            default -> false;
        };
    }

    /**
     * 대리 작성(다른 이름으로 게시) 사용 및 실제 작성자 열람 권한:
     * 최고관리자 OR "게시판 관리" 관리자메뉴 편집권한 보유 OR 해당 게시판 can_manage 권한
     */
    @Transactional(readOnly = true)
    public boolean canUseAliasWriter(Long boardId, Long memberId) {
        if (memberId == null) return false;
        if (memberGroupRepository.existsSuperAdminGroupByMemberId(memberId)) return true;
        List<Long> groupIds = memberGroupRepository.findGroupIdsByMemberId(memberId);
        if (groupIds.isEmpty()) return false;
        Long boardAdminMenuId = adminMenuRepository.findByUrl(BOARD_ADMIN_MENU_URL)
                .map(AdminMenu::getId).orElse(null);
        if (boardAdminMenuId != null
                && adminMenuGroupPermissionRepository.existsEditPermission(boardAdminMenuId, groupIds)) {
            return true;
        }
        return boardGroupPermissionRepository.existsManagePermission(boardId, groupIds);
    }

    /** 대리 작성 요청 이름이 실제로 사용 가능한지: 게시판 설정 on + 사용자 권한 + 등록된 이름 목록에 존재 */
    @Transactional(readOnly = true)
    public boolean isValidAliasName(Long boardId, Long memberId, String aliasName) {
        Board board = boardRepository.findById(boardId).orElse(null);
        if (board == null || !Boolean.TRUE.equals(board.getUseAliasWriter())) return false;
        if (!canUseAliasWriter(boardId, memberId)) return false;
        return boardWriterIdentityRepository.findByBoardIdOrderBySortOrderAsc(boardId).stream()
                .anyMatch(i -> i.getName().equals(aliasName));
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

    // 대리 작성 이름 목록: id 안정성이 필요 없으므로(게시글은 문자열로 이름만 저장) 단순 전체 교체
    private void saveIdentities(Long boardId, List<BoardCreateRequest.IdentityRequest> reqs) {
        boardWriterIdentityRepository.deleteByBoardId(boardId);
        if (reqs == null || reqs.isEmpty()) return;
        int order = 0;
        for (BoardCreateRequest.IdentityRequest r : reqs) {
            if (r.getName() == null || r.getName().isBlank()) { order++; continue; }
            boardWriterIdentityRepository.save(BoardWriterIdentity.builder()
                    .boardId(boardId)
                    .name(r.getName().trim())
                    .sortOrder(order)
                    .build());
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
