package com.corum.backend.dto.content;

import com.corum.backend.domain.content.ContentPageHistory;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ContentPageHistoryResponse {

    private final Long id;
    private final Long contentPageId;
    private final String content;
    private final Long createdBy;
    private final LocalDateTime createdAt;

    public ContentPageHistoryResponse(ContentPageHistory history) {
        this.id = history.getId();
        this.contentPageId = history.getContentPageId();
        this.content = history.getContent();
        this.createdBy = history.getCreatedBy();
        this.createdAt = history.getCreatedAt();
    }
}
