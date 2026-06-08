package com.corum.backend.dto.menu;

import com.corum.backend.domain.menu.Menu;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class MenuResponse {

    private final Long id;
    private final Long parentId;
    private final String name;
    private final String description;
    private final String menuType;
    private final String pageType;
    private final Long targetId;
    private final String url;          // 실제 접근 URL (resolveUrl 결과)
    private final Boolean newWindow;
    private final Integer sortOrder;
    private final Boolean isHidden;
    private final Boolean hideIfNoPermission;
    private final String accessType;
    private final Boolean isActive;
    private final List<Long> allowedGroupIds;
    private final List<MenuResponse> children;
    private final boolean hasNew;      // NEW 뱃지 여부 (추후 구현)

    public MenuResponse(Menu menu, List<Long> allowedGroupIds) {
        this(menu, allowedGroupIds, false);
    }

    public MenuResponse(Menu menu, List<Long> allowedGroupIds, boolean hasNew) {
        this.id = menu.getId();
        this.parentId = menu.getParentId();
        this.name = menu.getName();
        this.description = menu.getDescription();
        this.menuType = menu.getMenuType();
        this.pageType = menu.getPageType();
        this.targetId = menu.getTargetId();
        this.url = menu.resolveUrl();
        this.newWindow = menu.getNewWindow();
        this.sortOrder = menu.getSortOrder();
        this.isHidden = menu.getIsHidden();
        this.hideIfNoPermission = menu.getHideIfNoPermission();
        this.accessType = menu.getAccessType();
        this.isActive = menu.getIsActive();
        this.allowedGroupIds = allowedGroupIds;
        this.children = new ArrayList<>();
        this.hasNew = hasNew;
    }

    public void addChild(MenuResponse child) {
        this.children.add(child);
    }
}
