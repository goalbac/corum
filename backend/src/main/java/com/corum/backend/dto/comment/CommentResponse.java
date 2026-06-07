package com.corum.backend.dto.comment;

import com.corum.backend.domain.comment.Comment;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
public class CommentResponse {

    private final Long id;
    private final Long postId;
    private final Long parentId;
    private final Long memberId;
    private final String writerName;
    private final String content;
    private final Integer depth;
    private final Boolean isDeleted;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final List<CommentResponse> children;

    public CommentResponse(Comment comment) {
        this.id = comment.getId();
        this.postId = comment.getPostId();
        this.parentId = comment.getParentId();
        this.memberId = comment.getMemberId();
        this.writerName = comment.getWriterName();
        this.content = comment.getContent();
        this.depth = comment.getDepth();
        this.isDeleted = comment.getIsDeleted();
        this.createdAt = comment.getCreatedAt();
        this.updatedAt = comment.getUpdatedAt();
        this.children = new ArrayList<>();
    }

    public void addChild(CommentResponse child) {
        this.children.add(child);
    }
}
