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
    private final Boolean isNotice;
    private final Integer viewCount;
    private final Integer likeCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final List<FileResponse> files;
    private final boolean liked;     // 현재 로그인 사용자 좋아요 여부
    private final int commentCount;

    public PostResponse(Post post, List<FileResponse> files, boolean liked, int commentCount) {
        this.id = post.getId();
        this.boardId = post.getBoardId();
        this.memberId = post.getMemberId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.writerName = post.getWriterName();
        this.isNotice = post.getIsNotice();
        this.viewCount = post.getViewCount();
        this.likeCount = post.getLikeCount();
        this.createdAt = post.getCreatedAt();
        this.updatedAt = post.getUpdatedAt();
        this.files = files;
        this.liked = liked;
        this.commentCount = commentCount;
    }
}
