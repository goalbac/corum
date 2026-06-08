package com.corum.backend.dto.content;

import com.corum.backend.domain.content.ContentPage;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ContentPageResponse {

    private final Long id;
    private final Long menuId;
    private final String title;
    private final String content;
    private final Long createdBy;
    private final Long updatedBy;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ContentPageResponse(ContentPage page) {
        this.id = page.getId();
        this.menuId = page.getMenuId();
        this.title = page.getTitle();
        this.content = page.getContent();
        this.createdBy = page.getCreatedBy();
        this.updatedBy = page.getUpdatedBy();
        this.createdAt = page.getCreatedAt();
        this.updatedAt = page.getUpdatedAt();
    }
}
