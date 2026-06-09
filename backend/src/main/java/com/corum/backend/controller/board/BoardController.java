package com.corum.backend.controller.board;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.board.BoardCreateRequest;
import com.corum.backend.dto.board.BoardResponse;
import com.corum.backend.dto.comment.CommentCreateRequest;
import com.corum.backend.dto.comment.CommentResponse;
import com.corum.backend.dto.post.AdjacentPostsResponse;
import com.corum.backend.dto.post.PostCreateRequest;
import com.corum.backend.dto.post.PostResponse;
import com.corum.backend.dto.post.PostSummaryResponse;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.dto.file.FileResponse;
import com.corum.backend.domain.board.BoardGroupPermissionRepository;
import com.corum.backend.domain.group.MemberGroupRepository;
import com.corum.backend.service.board.BoardService;
import com.corum.backend.service.comment.CommentService;
import com.corum.backend.service.file.FileStorageService;
import com.corum.backend.service.log.OperationLogService;
import com.corum.backend.service.post.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final PostService postService;
    private final CommentService commentService;
    private final FileStorageService fileStorageService;
    private final OperationLogService operationLogService;
    private final MemberGroupRepository memberGroupRepository;
    private final BoardGroupPermissionRepository boardGroupPermissionRepository;

    // ===== 게시판 =====

    @GetMapping("/api/boards")
    public ApiResponse<List<BoardResponse>> getBoards() {
        return ApiResponse.ok(boardService.getBoards());
    }

    @GetMapping("/api/boards/{boardId}")
    public ApiResponse<BoardResponse> getBoard(@PathVariable Long boardId) {
        return ApiResponse.ok(boardService.getBoard(boardId));
    }

    @PostMapping("/api/boards")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BoardResponse> createBoard(@Valid @RequestBody BoardCreateRequest request) {
        return ApiResponse.ok(boardService.createBoard(request));
    }

    @PutMapping("/api/boards/{boardId}")
    public ApiResponse<BoardResponse> updateBoard(
            @PathVariable Long boardId,
            @RequestBody BoardCreateRequest request) {
        return ApiResponse.ok(boardService.updateBoard(boardId, request));
    }

    @DeleteMapping("/api/boards/{boardId}")
    public ApiResponse<Void> deleteBoard(@PathVariable Long boardId) {
        boardService.deleteBoard(boardId);
        return ApiResponse.ok("게시판이 삭제되었습니다.");
    }

    // ===== 게시글 =====

    @GetMapping("/api/boards/{boardId}/posts")
    public ApiResponse<Page<PostSummaryResponse>> getPosts(
            @PathVariable Long boardId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest) {

        Pageable pageable = PageRequest.of(page, size);
        Page<PostSummaryResponse> result = postService.getPosts(boardId, searchType, keyword, pageable);
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        operationLogService.search(memberId, keyword, searchType, (int) result.getTotalElements(), httpRequest);
        return ApiResponse.ok(result);
    }

    @GetMapping("/api/boards/{boardId}/posts/{postId}")
    public ApiResponse<PostResponse> getPost(
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        return ApiResponse.ok(postService.getPost(postId, memberId));
    }

    @PostMapping("/api/boards/{boardId}/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostResponse> createPost(
            @PathVariable Long boardId,
            @Valid @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest) {

        return ApiResponse.ok(postService.createPost(
                boardId, request, userDetails.getMemberId(), null, httpRequest));
    }

    // 게시글 파일 업로드 (글 작성 후 별도 호출)
    @PostMapping("/api/posts/{postId}/files")
    public ApiResponse<List<FileResponse>> uploadPostFiles(
            @PathVariable Long postId,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ApiResponse.ok(fileStorageService.uploadFiles(
                "POST", postId, files, userDetails.getMemberId()));
    }

    // 게시글 파일 단건 삭제
    @DeleteMapping("/api/posts/{postId}/files/{fileId}")
    public ApiResponse<Void> deletePostFile(
            @PathVariable Long postId,
            @PathVariable Long fileId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        fileStorageService.deleteFile(fileId);
        return ApiResponse.ok("파일이 삭제되었습니다.");
    }

    @GetMapping("/api/boards/{boardId}/posts/{postId}/adjacent")
    public ApiResponse<AdjacentPostsResponse> getAdjacentPosts(
            @PathVariable Long boardId,
            @PathVariable Long postId) {
        return ApiResponse.ok(postService.getAdjacentPosts(boardId, postId));
    }

    @PutMapping("/api/boards/{boardId}/posts/{postId}")
    public ApiResponse<PostResponse> updatePost(
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @Valid @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        boolean canManage = hasManagePermission(boardId, userDetails.getMemberId());
        return ApiResponse.ok(postService.updatePost(postId, request, userDetails.getMemberId(), canManage));
    }

    @DeleteMapping("/api/boards/{boardId}/posts/{postId}")
    public ApiResponse<Void> deletePost(
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        boolean canManage = hasManagePermission(boardId, userDetails.getMemberId());
        postService.deletePost(postId, userDetails.getMemberId(), canManage);
        return ApiResponse.ok("게시글이 삭제되었습니다.");
    }

    /** 게시판 관리 권한: 전체 관리자(ADMIN 그룹) OR 해당 게시판의 can_manage 권한 */
    private boolean hasManagePermission(Long boardId, Long memberId) {
        if (memberGroupRepository.existsAdminGroupByMemberId(memberId)) return true;
        java.util.List<Long> groupIds = memberGroupRepository.findGroupIdsByMemberId(memberId);
        if (groupIds.isEmpty()) return false;
        return boardGroupPermissionRepository.existsManagePermission(boardId, groupIds);
    }

    @PostMapping("/api/boards/{boardId}/posts/{postId}/like")
    public ApiResponse<Map<String, Object>> toggleLike(
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        boolean liked = postService.toggleLike(postId, userDetails.getMemberId());
        return ApiResponse.ok(Map.of("liked", liked));
    }

    // ===== 댓글 =====

    @GetMapping("/api/boards/{boardId}/posts/{postId}/comments")
    public ApiResponse<List<CommentResponse>> getComments(
            @PathVariable Long boardId,
            @PathVariable Long postId) {
        return ApiResponse.ok(commentService.getComments(postId));
    }

    @PostMapping("/api/boards/{boardId}/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> createComment(
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest) {

        return ApiResponse.ok(commentService.createComment(
                postId, request, userDetails.getMemberId(), httpRequest));
    }

    @PutMapping("/api/boards/{boardId}/posts/{postId}/comments/{commentId}")
    public ApiResponse<CommentResponse> updateComment(
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ApiResponse.ok(commentService.updateComment(
                commentId, body.get("content"), userDetails.getMemberId()));
    }

    @DeleteMapping("/api/boards/{boardId}/posts/{postId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        commentService.deleteComment(commentId, userDetails.getMemberId(), false);
        return ApiResponse.ok("댓글이 삭제되었습니다.");
    }
}
