package com.corum.backend.dto.message;

import com.corum.backend.domain.message.Message;
import com.corum.backend.dto.file.FileResponse;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class ChatMessageResponse {
    private final Long id;
    private final String content;
    private final Long senderId;
    // Boolean (wrapper) → getter getIsMine() → Jackson serializes as "isMine"
    // (primitive boolean + Lombok would generate isMine() → Jackson strips "is" prefix → "mine")
    private final Boolean isMine;
    private final Boolean isRead;
    private final LocalDateTime createdAt;
    private final List<FileResponse> files;

    public ChatMessageResponse(Message message, Boolean isMine, Boolean isRead, List<FileResponse> files) {
        this.id        = message.getId();
        this.content   = message.getContent();
        this.senderId  = message.getSenderId();
        this.isMine    = isMine;
        this.isRead    = isRead;
        this.createdAt = message.getCreatedAt();
        this.files     = files != null ? files : List.of();
    }
}
