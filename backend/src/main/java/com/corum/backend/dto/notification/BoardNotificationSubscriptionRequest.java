package com.corum.backend.dto.notification;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BoardNotificationSubscriptionRequest {
    private Boolean notifyNewPost;
    private Boolean notifyNewComment;
}
