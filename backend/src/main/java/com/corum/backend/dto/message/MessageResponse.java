package com.corum.backend.dto.message;

import com.corum.backend.domain.message.Message;
import com.corum.backend.domain.message.MessageRecipient;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MessageResponse {

    private final Long id;
    private final Long messageId;
    private final String title;
    private final String content;
    private final Long senderId;
    private final String senderName;
    private final Boolean isRead;
    private final LocalDateTime readAt;
    private final Boolean isNotice;
    private final LocalDateTime createdAt;

    public MessageResponse(MessageRecipient mr, Message message, String senderName) {
        this.id          = mr.getId();
        this.messageId   = message.getId();
        this.title       = message.getTitle();
        this.content     = message.getContent();
        this.senderId    = message.getSenderId();
        this.senderName  = senderName;
        this.isRead      = mr.getIsRead();
        this.readAt      = mr.getReadAt();
        this.isNotice    = message.getIsNotice();
        this.createdAt   = message.getCreatedAt();
    }
}
