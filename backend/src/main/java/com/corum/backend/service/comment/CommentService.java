package com.corum.backend.service.comment;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.comment.Comment;
import com.corum.backend.domain.comment.CommentRepository;
import com.corum.backend.domain.member.MemberRepository;
import com.corum.backend.dto.comment.CommentCreateRequest;
import com.corum.backend.dto.comment.CommentResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;

    private static final int MAX_DEPTH = 2; // 0-based (0,1,2 = 3뎁스)

    // ===== 댓글 목록 (트리 구조) =====
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);

        Map<Long, CommentResponse> map = comments.stream()
                .collect(Collectors.toMap(Comment::getId, CommentResponse::new));

        List<CommentResponse> roots = new ArrayList<>();
        comments.forEach(c -> {
            if (c.getParentId() == null) {
                roots.add(map.get(c.getId()));
            } else {
                CommentResponse parent = map.get(c.getParentId());
                if (parent != null) {
                    parent.addChild(map.get(c.getId()));
                }
            }
        });

        return roots;
    }

    // ===== 댓글 작성 =====
    @Transactional
    public CommentResponse createComment(Long postId, CommentCreateRequest request,
                                         Long memberId, HttpServletRequest httpRequest) {
        int depth = 0;

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> BusinessException.notFound("상위 댓글을 찾을 수 없습니다."));

            if (parent.getIsDeleted()) {
                throw new BusinessException("삭제된 댓글에는 답글을 달 수 없습니다.");
            }

            depth = parent.getDepth() + 1;
            if (depth > MAX_DEPTH) {
                throw new BusinessException("댓글은 최대 3단계까지만 작성할 수 있습니다.");
            }
        }

        String writerName = memberRepository.findById(memberId)
                .map(m -> m.getName())
                .orElse("알 수 없음");

        String ip = httpRequest.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = httpRequest.getRemoteAddr();

        Comment comment = Comment.builder()
                .postId(postId)
                .parentId(request.getParentId())
                .memberId(memberId)
                .writerName(writerName)
                .content(request.getContent())
                .depth(depth)
                .clientIp(ip)
                .build();

        return new CommentResponse(commentRepository.save(comment));
    }

    // ===== 댓글 수정 =====
    @Transactional
    public CommentResponse updateComment(Long commentId, String content, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> BusinessException.notFound("댓글을 찾을 수 없습니다."));

        if (!comment.getMemberId().equals(memberId)) {
            throw BusinessException.forbidden("수정 권한이 없습니다.");
        }

        comment.update(content);
        return new CommentResponse(comment);
    }

    // ===== 댓글 삭제 =====
    @Transactional
    public void deleteComment(Long commentId, Long memberId, boolean isAdmin) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> BusinessException.notFound("댓글을 찾을 수 없습니다."));

        if (!isAdmin && !comment.getMemberId().equals(memberId)) {
            throw BusinessException.forbidden("삭제 권한이 없습니다.");
        }

        // 대댓글이 있으면 소프트 삭제, 없으면 하드 삭제
        if (commentRepository.existsByParentIdAndIsDeletedFalse(commentId)) {
            comment.delete();
        } else {
            commentRepository.delete(comment);
        }
    }
}
