package com.corum.backend.service.board;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.board.Board;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardGroupPermissionRepository boardGroupPermissionRepository;
    private final MemberGroupRepository memberGroupRepository;
    private final PostRepository postRepository;

    // ===== 게시판 목록 =====
    @Transactional(readOnly = true)
    public List<BoardResponse> getBoards() {
        return boardRepository.findByIsActiveTrueOrderByIdAsc().stream()
                .map(b -> new BoardResponse(b, boardGroupPermissionRepository.findByBoardId(b.getId())))
                .collect(Collectors.toList());
    }

    // ===== 게시판 단건 =====
    @Transactional(readOnly = true)
    public BoardResponse getBoard(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("게시판을 찾을 수 없습니다."));
        List<BoardGroupPermission> permissions = boardGroupPermissionRepository.findByBoardId(id);
        return new BoardResponse(board, permissions);
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
                .build();

        Board saved = boardRepository.save(board);
        savePermissions(saved.getId(), request.getPermissions());

        List<BoardGroupPermission> permissions = boardGroupPermissionRepository.findByBoardId(saved.getId());
        return new BoardResponse(saved, permissions);
    }

    // ===== 게시판 수정 =====
    @Transactional
    public BoardResponse updateBoard(Long id, BoardCreateRequest request) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("게시판을 찾을 수 없습니다."));
        board.update(request.getName(), request.getBoardType(), request.getUseComment(),
                request.getUseLike(), request.getUseAnonymous(), request.getUseNotice(),
                request.getNoticeCountLimit(), request.getFileMaxSizeMb(),
                request.getFileAllowedExtensions(), request.getFileMaxCount(), request.getIsActive());
        if (request.getPermissions() != null) {
            boardGroupPermissionRepository.deleteByBoardId(id);
            savePermissions(id, request.getPermissions());
        }
        List<BoardGroupPermission> permissions = boardGroupPermissionRepository.findByBoardId(id);
        return new BoardResponse(board, permissions);
    }

    // ===== 게시판 삭제 =====
    @Transactional
    public void deleteBoard(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("게시판을 찾을 수 없습니다."));
        boardGroupPermissionRepository.deleteByBoardId(id);
        boardRepository.delete(board);
    }

    // ===== 권한 확인 =====
    @Transactional(readOnly = true)
    public boolean hasPermission(Long boardId, List<Long> memberGroupIds, String permType) {
        List<BoardGroupPermission> allPerms = boardGroupPermissionRepository.findByBoardId(boardId);
        if (allPerms.isEmpty()) {
            return switch (permType) {
                case "READ", "DOWNLOAD" -> true;
                case "WRITE", "COMMENT" -> !memberGroupIds.isEmpty();
                default -> false;
            };
        }
        if (memberGroupIds.isEmpty()) return false;
        List<BoardGroupPermission> perms = boardGroupPermissionRepository
                .findByBoardIdAndGroupIds(boardId, memberGroupIds);
        return perms.stream().anyMatch(p -> switch (permType) {
            case "READ"     -> p.getCanRead();
            case "WRITE"    -> p.getCanWrite();
            case "COMMENT"  -> p.getCanComment();
            case "DOWNLOAD" -> p.getCanDownload();
            default -> false;
        });
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(Long boardId, Long memberId, String permType) {
        if (memberId != null && memberGroupRepository.existsAdminGroupByMemberId(memberId)) {
            return true;
        }
        List<Long> groupIds = memberId == null ? List.of() : memberGroupRepository.findGroupIdsByMemberId(memberId);
        return hasPermission(boardId, groupIds, permType);
    }

    @Transactional(readOnly = true)
    public void requirePermission(Long boardId, Long memberId, String permType) {
        if (!hasPermission(boardId, memberId, permType)) {
            throw BusinessException.forbidden("게시판 권한이 없습니다.");
        }
    }

    @Transactional(readOnly = true)
    public Long getPostBoardId(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> BusinessException.notFound("게시글을 찾을 수 없습니다."))
                .getBoardId();
    }

    // ===== 내부 메서드 =====
    private void savePermissions(Long boardId, List<BoardCreateRequest.BoardPermissionRequest> reqs) {
        if (reqs == null || reqs.isEmpty()) return;
        List<BoardGroupPermission> permissions = reqs.stream()
                .map(r -> BoardGroupPermission.builder()
                        .boardId(boardId)
                        .groupId(r.getGroupId())
                        .canRead(r.getCanRead())
                        .canWrite(r.getCanWrite())
                        .canComment(r.getCanComment())
                        .canDownload(r.getCanDownload())
                        .build())
                .collect(Collectors.toList());
        boardGroupPermissionRepository.saveAll(permissions);
    }
}
