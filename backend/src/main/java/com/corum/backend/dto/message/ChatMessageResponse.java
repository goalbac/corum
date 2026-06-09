package com.corum.backend.dto.message;

import com.corum.backend.domain.message.Message;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatMessageResponse {
    private final Long id;
    private final String content;
    private final Long senderId;
    private final boolean isMine;
    private final boolean isRead;
    private final LocalDateTime createdAt;

    public ChatMessageResponse(Message message, boolean isMine, boolean isRead) {
        this.id        = message.getId();
        this.content   = message.getContent();
        this.senderId  = message.getSenderId();
        this.isMine    = isMine;
        this.isRead    = isRead;
        this.createdAt = message.getCreatedAt();
    }
}
