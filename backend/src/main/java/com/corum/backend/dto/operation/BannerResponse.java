package com.corum.backend.dto.operation;

import com.corum.backend.domain.operation.Banner;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BannerResponse {
    private final Long id;
    private final String title;
    private final String content;
    private final String linkUrl;
    private final Boolean linkNewWindow;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final Boolean isActive;
    private final String bgColor;
    private final String textAlign;
    private final Long createdBy;
    private final String createdByName;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public BannerResponse(Banner banner, String createdByName) {
        this.id = banner.getId();
        this.title = banner.getTitle();
        this.content = banner.getContent();
        this.linkUrl = banner.getLinkUrl();
        this.linkNewWindow = banner.getLinkNewWindow();
        this.startAt = banner.getStartAt();
        this.endAt = banner.getEndAt();
        this.isActive = banner.getIsActive();
        this.bgColor = banner.getBgColor();
        this.textAlign = banner.getTextAlign();
        this.createdBy = banner.getCreatedBy();
        this.createdByName = createdByName;
        this.createdAt = banner.getCreatedAt();
        this.updatedAt = banner.getUpdatedAt();
    }
}
