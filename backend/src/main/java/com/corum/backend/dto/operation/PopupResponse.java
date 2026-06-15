package com.corum.backend.dto.operation;

import com.corum.backend.domain.operation.Popup;
import com.corum.backend.domain.operation.PopupTargetPage;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class PopupResponse {
    private final Long id;
    private final String title;
    private final String contentType;
    private final String content;
    private final String imageUrl;
    private final String linkUrl;
    private final Boolean linkNewWindow;
    private final String position;
    private final Integer priority;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final Boolean isActive;
    private final Long createdBy;
    private final String createdByName;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final String targetType;
    private final List<Long> targetMenuIds;

    public PopupResponse(Popup popup, List<PopupTargetPage> targets, String createdByName) {
        this.id = popup.getId();
        this.title = popup.getTitle();
        this.contentType = popup.getContentType();
        this.content = popup.getContent();
        this.imageUrl = popup.getImageUrl();
        this.linkUrl = popup.getLinkUrl();
        this.linkNewWindow = popup.getLinkNewWindow();
        this.position = popup.getPosition();
        this.priority = popup.getPriority();
        this.startAt = popup.getStartAt();
        this.endAt = popup.getEndAt();
        this.isActive = popup.getIsActive();
        this.createdBy = popup.getCreatedBy();
        this.createdByName = createdByName;
        this.createdAt = popup.getCreatedAt();
        this.updatedAt = popup.getUpdatedAt();
        this.targetType = targets.stream().findFirst().map(PopupTargetPage::getTargetType).orElse("ALL");
        this.targetMenuIds = targets.stream()
                .map(PopupTargetPage::getTargetMenuId)
                .filter(id -> id != null)
                .toList();
    }
}
