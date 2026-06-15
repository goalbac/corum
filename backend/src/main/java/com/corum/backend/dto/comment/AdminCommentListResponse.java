package com.corum.backend.dto.comment;

import com.corum.backend.domain.comment.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminCommentListResponse {
    private Long id;
    private Long postId;
    private String postTitle;
    private Long boardId;
    private String boardName;
    private Long memberId;
    private String writerName;
    private String profileImageUrl;
    private String content;
    private Integer depth;
    private Boolean isDeleted;
    private LocalDateTime createdAt;

    public static AdminCommentListResponse of(Comment c, String postTitle,
                                              Long boardId, String boardName,
                                              String profileImageUrl) {
        return AdminCommentListResponse.builder()
                .id(c.getId())
                .postId(c.getPostId())
                .postTitle(postTitle)
                .boardId(boardId)
                .boardName(boardName)
                .memberId(c.getMemberId())
                .writerName(c.getWriterName())
                .profileImageUrl(profileImageUrl)
                .content(c.getContent())
                .depth(c.getDepth())
                .isDeleted(c.getIsDeleted())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
