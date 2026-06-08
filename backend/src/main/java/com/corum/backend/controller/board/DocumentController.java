package com.corum.backend.controller.board;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.document.DocumentFolderRequest;
import com.corum.backend.dto.document.DocumentFolderResponse;
import com.corum.backend.dto.document.DocumentVersionRequest;
import com.corum.backend.dto.document.DocumentVersionResponse;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.board.BoardService;
import com.corum.backend.service.document.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final BoardService boardService;

    @GetMapping("/api/boards/{boardId}/document-folders")
    public ApiResponse<List<DocumentFolderResponse>> getFolders(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boardService.requirePermission(boardId, memberId(userDetails), "READ");
        return ApiResponse.ok(documentService.getFolders(boardId));
    }

    @PostMapping("/api/boards/{boardId}/document-folders")
    public ApiResponse<DocumentFolderResponse> createFolder(
            @PathVariable Long boardId,
            @Valid @RequestBody DocumentFolderRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boardService.requirePermission(boardId, memberId(userDetails), "WRITE");
        return ApiResponse.ok(documentService.createFolder(boardId, request));
    }

    @PutMapping("/api/boards/{boardId}/document-folders/{folderId}")
    public ApiResponse<DocumentFolderResponse> updateFolder(
            @PathVariable Long boardId,
            @PathVariable Long folderId,
            @Valid @RequestBody DocumentFolderRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boardService.requirePermission(boardId, memberId(userDetails), "WRITE");
        return ApiResponse.ok(documentService.updateFolder(boardId, folderId, request));
    }

    @DeleteMapping("/api/boards/{boardId}/document-folders/{folderId}")
    public ApiResponse<Void> deleteFolder(
            @PathVariable Long boardId,
            @PathVariable Long folderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boardService.requirePermission(boardId, memberId(userDetails), "WRITE");
        documentService.deleteFolder(boardId, folderId);
        return ApiResponse.ok("문서 폴더가 삭제되었습니다.");
    }

    @GetMapping("/api/posts/{postId}/document-versions")
    public ApiResponse<List<DocumentVersionResponse>> getVersions(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long boardId = boardService.getPostBoardId(postId);
        boardService.requirePermission(boardId, memberId(userDetails), "READ");
        return ApiResponse.ok(documentService.getVersions(postId));
    }

    @PostMapping("/api/posts/{postId}/document-versions")
    public ApiResponse<DocumentVersionResponse> createVersion(
            @PathVariable Long postId,
            @Valid @RequestBody DocumentVersionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long boardId = boardService.getPostBoardId(postId);
        boardService.requirePermission(boardId, memberId(userDetails), "WRITE");
        return ApiResponse.ok(documentService.createVersion(postId, request, memberId(userDetails)));
    }

    private Long memberId(CustomUserDetails userDetails) {
        return userDetails == null ? null : userDetails.getMemberId();
    }
}
