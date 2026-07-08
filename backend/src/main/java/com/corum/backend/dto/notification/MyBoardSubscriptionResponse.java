package com.corum.backend.dto.notification;

import lombok.Getter;

@Getter
public class MyBoardSubscriptionResponse {
    private final Long boardId;
    private final String boardName;
    private final Long menuId;
    private final Boolean notifyNewPost;
    private final Boolean notifyNewComment;

    public MyBoardSubscriptionResponse(Long boardId, String boardName, Long menuId,
                                        Boolean notifyNewPost, Boolean notifyNewComment) {
        this.boardId = boardId;
        this.boardName = boardName;
        this.menuId = menuId;
        this.notifyNewPost = notifyNewPost;
        this.notifyNewComment = notifyNewComment;
    }
}
