package com.corum.backend.dto.dashboard;

import com.corum.backend.domain.dashboard.DashboardWidget;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class DashboardWidgetResponse {

    private final Long id;
    private final String widgetType;
    private final String title;
    private final Long targetBoardId;
    private final String targetBoardName;
    private final Integer postCount;
    private final Integer sortOrder;
    private final Boolean isActive;
    private final String extraConfig;
    private final LocalDateTime updatedAt;
    private final Long updatedBy;
    private final List<DashboardPostResponse> posts;
    private final DashboardStatsResponse stats;
    private final List<DashboardCalendarEventResponse> calendarEvents;

    public DashboardWidgetResponse(DashboardWidget widget, String targetBoardName,
                                   List<DashboardPostResponse> posts, DashboardStatsResponse stats) {
        this(widget, targetBoardName, posts, stats, List.of());
    }

    public DashboardWidgetResponse(DashboardWidget widget, String targetBoardName,
                                   List<DashboardPostResponse> posts, DashboardStatsResponse stats,
                                   List<DashboardCalendarEventResponse> calendarEvents) {
        this.id             = widget.getId();
        this.widgetType     = widget.getWidgetType();
        this.title          = widget.getTitle();
        this.targetBoardId  = widget.getTargetBoardId();
        this.targetBoardName = targetBoardName;
        this.postCount      = widget.getPostCount();
        this.sortOrder      = widget.getSortOrder();
        this.isActive       = widget.getIsActive();
        this.extraConfig    = widget.getExtraConfig();
        this.updatedAt      = widget.getUpdatedAt();
        this.updatedBy      = widget.getUpdatedBy();
        this.posts          = posts;
        this.stats          = stats;
        this.calendarEvents = calendarEvents;
    }
}
