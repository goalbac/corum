package com.corum.backend.dto.post;

import com.corum.backend.domain.post.Post;
import com.corum.backend.dto.file.FileResponse;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class PostResponse {

    private final Long id;
    private final Long boardId;
    private final Long memberId;
    private final String title;
    private final String content;
    private final String writerName;
    private final String writerProfileImageUrl;
    private final Boolean isNotice;
    private final Integer viewCount;
    private final Integer likeCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final List<FileResponse> files;
    private final boolean liked;
    private final int commentCount;
    private final Long categoryId;
    private final String categoryName;

    public PostResponse(Post post, List<FileResponse> files, boolean liked, int commentCount, String writerProfileImageUrl) {
        this(post, files, liked, commentCount, writerProfileImageUrl, null);
    }

    public PostResponse(Post post, List<FileResponse> files, boolean liked, int commentCount,
                        String writerProfileImageUrl, String categoryName) {
        this.id = post.getId();
        this.boardId = post.getBoardId();
        this.memberId = post.getMemberId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.writerName = post.getWriterName();
        this.writerProfileImageUrl = writerProfileImageUrl;
        this.isNotice = post.getIsNotice();
        this.viewCount = post.getViewCount();
        this.likeCount = post.getLikeCount();
        this.createdAt = post.getCreatedAt();
        this.updatedAt = post.getUpdatedAt();
        this.files = files;
        this.liked = liked;
        this.commentCount = commentCount;
        this.categoryId = post.getCategoryId();
        this.categoryName = categoryName;
    }
}
