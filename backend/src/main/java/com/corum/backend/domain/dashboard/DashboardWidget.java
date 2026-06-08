package com.corum.backend.domain.dashboard;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dashboard_widgets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class DashboardWidget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "widget_type", nullable = false, length = 50)
    private String widgetType;

    @Column(length = 200)
    private String title;

    @Column(name = "target_board_id")
    private Long targetBoardId;

    @Column(name = "post_count", nullable = false)
    private Integer postCount;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "extra_config", columnDefinition = "TEXT")
    private String extraConfig;

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "updated_by")
    private Long updatedBy;

    public void update(String widgetType, String title, Long targetBoardId, Integer postCount,
                       Integer sortOrder, Boolean isActive, String extraConfig, Long updatedBy) {
        this.widgetType = widgetType;
        this.title = title;
        this.targetBoardId = targetBoardId;
        this.postCount = postCount;
        this.sortOrder = sortOrder;
        this.isActive = isActive;
        this.extraConfig = extraConfig;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }
}
