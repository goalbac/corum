package com.corum.backend.dto.notification;

import lombok.Getter;

@Getter
public class BoardNotificationSubscriptionResponse {
    private final Long boardId;
    private final Boolean notifyNewPost;
    private final Boolean notifyNewComment;

    public BoardNotificationSubscriptionResponse(Long boardId, Boolean notifyNewPost, Boolean notifyNewComment) {
        this.boardId = boardId;
        this.notifyNewPost = notifyNewPost;
        this.notifyNewComment = notifyNewComment;
    }
}
