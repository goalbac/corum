package com.corum.backend.dto.notification;

import com.corum.backend.domain.notification.Notification;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NotificationResponse {
    private final Long id;
    private final String type;
    private final String title;
    private final String content;
    private final String linkUrl;
    private final Boolean isRead;
    private final LocalDateTime createdAt;

    public NotificationResponse(Notification n) {
        this.id        = n.getId();
        this.type      = n.getType();
        this.title     = n.getTitle();
        this.content   = n.getContent();
        this.linkUrl   = n.getLinkUrl();
        this.isRead    = n.getIsRead();
        this.createdAt = n.getCreatedAt();
    }
}
