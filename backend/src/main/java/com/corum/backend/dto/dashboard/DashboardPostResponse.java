package com.corum.backend.dto.dashboard;

import com.corum.backend.domain.post.Post;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DashboardPostResponse {

    private final Long id;
    private final Long boardId;
    private final String boardName;
    private final String title;
    private final String writerName;
    private final LocalDateTime createdAt;

    public DashboardPostResponse(Post post) {
        this(post, null);
    }

    public DashboardPostResponse(Post post, String boardName) {
        this.id = post.getId();
        this.boardId = post.getBoardId();
        this.boardName = boardName;
        this.title = post.getTitle();
        this.writerName = post.getWriterName();
        this.createdAt = post.getCreatedAt();
    }
}
