package com.corum.backend.dto.comment;

import com.corum.backend.domain.comment.Comment;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public class CommentResponse {

    private final Long id;
    private final Long postId;
    private final Long parentId;
    private final Long memberId;
    private final String writerName;
    private final String writerProfileImageUrl;
    private final String content;
    private final Integer depth;
    private final Boolean isDeleted;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final List<CommentResponse> children;

    /** 리액션 집계: emoji_type → 개수 */
    private Map<String, Integer> reactions;

    /** 현재 사용자가 누른 리액션 타입 목록 */
    private Set<String> myReactions;

    public CommentResponse(Comment comment) {
        this(comment, null);
    }

    public CommentResponse(Comment comment, String writerProfileImageUrl) {
        this.id = comment.getId();
        this.postId = comment.getPostId();
        this.parentId = comment.getParentId();
        this.memberId = comment.getMemberId();
        this.writerName = comment.getWriterName();
        this.writerProfileImageUrl = writerProfileImageUrl;
        this.content = comment.getContent();
        this.depth = comment.getDepth();
        this.isDeleted = comment.getIsDeleted();
        this.createdAt = comment.getCreatedAt();
        this.updatedAt = comment.getUpdatedAt();
        this.children = new ArrayList<>();
        this.reactions = Map.of();
        this.myReactions = Set.of();
    }

    public void addChild(CommentResponse child) {
        this.children.add(child);
    }

    public void setReactions(Map<String, Integer> reactions, Set<String> myReactions) {
        this.reactions = reactions != null ? reactions : Map.of();
        this.myReactions = myReactions != null ? myReactions : Set.of();
    }
}
