package com.corum.backend.service.board;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.board.Board;
import com.corum.backend.domain.board.BoardGroupPermission;
import com.corum.backend.domain.board.BoardGroupPermissionRepository;
import com.corum.backend.domain.board.BoardRepository;
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
        boardGroupPermissionRepository.deleteByBoardId(id);
        savePermissions(id, request.getPermissions());
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

    // ===== 내부 메서드 =====
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
