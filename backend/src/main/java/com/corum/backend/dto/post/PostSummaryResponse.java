package com.corum.backend.dto.post;

import com.corum.backend.domain.post.Post;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PostSummaryResponse {

    private final Long id;
    private final Long boardId;
    private final String title;
    private final String writerName;
    private final Boolean isNotice;
    private final Integer viewCount;
    private final Integer likeCount;
    private final LocalDateTime createdAt;
    private final int commentCount;
    private final boolean hasFile;
    private final String thumbnailUrl;
    private final Long rowNum;

    public PostSummaryResponse(Post post, int commentCount, boolean hasFile) {
        this(post, commentCount, hasFile, null, null);
    }

    public PostSummaryResponse(Post post, int commentCount, boolean hasFile, String thumbnailUrl) {
        this(post, commentCount, hasFile, thumbnailUrl, null);
    }

    public PostSummaryResponse(Post post, int commentCount, boolean hasFile, String thumbnailUrl, Long rowNum) {
        this.id = post.getId();
        this.boardId = post.getBoardId();
        this.title = post.getTitle();
        this.writerName = post.getWriterName();
        this.isNotice = post.getIsNotice();
        this.viewCount = post.getViewCount();
        this.likeCount = post.getLikeCount();
        this.createdAt = post.getCreatedAt();
        this.commentCount = commentCount;
        this.hasFile = hasFile;
        this.thumbnailUrl = thumbnailUrl;
        this.rowNum = rowNum;
    }
}
