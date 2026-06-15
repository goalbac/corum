package com.corum.backend.controller.admin;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.domain.board.Board;
import com.corum.backend.domain.board.BoardRepository;
import com.corum.backend.domain.comment.Comment;
import com.corum.backend.domain.comment.CommentRepository;
import com.corum.backend.domain.member.Member;
import com.corum.backend.domain.member.MemberRepository;
import com.corum.backend.domain.post.Post;
import com.corum.backend.domain.post.PostRepository;
import com.corum.backend.dto.comment.AdminCommentBatchRequest;
import com.corum.backend.dto.comment.AdminCommentListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;

    // ===== 최근 댓글 목록 =====
    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> getComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Comment> commentPage = commentRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size));
        List<Comment> comments = commentPage.getContent();

        // 배치 조회: Post, Board, Member
        List<Long> postIds = comments.stream().map(Comment::getPostId).distinct().collect(Collectors.toList());
        Map<Long, Post> postMap = postRepository.findAllById(postIds).stream()
                .collect(Collectors.toMap(Post::getId, p -> p));

        List<Long> boardIds = postMap.values().stream().map(Post::getBoardId).distinct().collect(Collectors.toList());
        Map<Long, Board> boardMap = boardRepository.findAllById(boardIds).stream()
                .collect(Collectors.toMap(Board::getId, b -> b));

        List<Long> memberIds = comments.stream()
                .filter(c -> c.getMemberId() != null)
                .map(Comment::getMemberId).distinct().collect(Collectors.toList());
        Map<Long, Member> memberMap = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        List<AdminCommentListResponse> items = comments.stream().map(c -> {
            Post post = postMap.get(c.getPostId());
            String postTitle = post != null ? post.getTitle() : "";
            Long boardId = post != null ? post.getBoardId() : null;
            Board board = boardId != null ? boardMap.get(boardId) : null;
            String boardName = board != null ? board.getName() : "";
            Member member = c.getMemberId() != null ? memberMap.get(c.getMemberId()) : null;
            String profileImageUrl = member != null ? member.getProfileImageUrl() : null;
            return AdminCommentListResponse.of(c, postTitle, boardId, boardName, profileImageUrl);
        }).collect(Collectors.toList());

        return ApiResponse.ok(Map.of(
                "items", items,
                "totalElements", commentPage.getTotalElements(),
                "totalPages", commentPage.getTotalPages(),
                "page", page,
                "size", size
        ));
    }

    // ===== 배치 작성일시 수정 =====
    @PutMapping("/batch")
    @Transactional
    public ApiResponse<Void> batchUpdate(@RequestBody AdminCommentBatchRequest req) {
        if (req.getIds() == null || req.getIds().isEmpty() || req.getCreatedAt() == null)
            return ApiResponse.ok(null);
        for (Long id : req.getIds()) {
            commentRepository.updateCreatedAtById(id, req.getCreatedAt());
        }
        return ApiResponse.ok(null);
    }

    // ===== 배치 삭제 (하드 삭제) =====
    @DeleteMapping("/batch")
    @Transactional
    public ApiResponse<Void> batchDelete(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids != null && !ids.isEmpty()) {
            commentRepository.deleteAllById(ids);
        }
        return ApiResponse.ok(null);
    }
}
