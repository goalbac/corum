package com.corum.backend.dto.dashboard;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardWidgetRequest {

    @NotBlank(message = "위젯 유형을 선택해주세요.")
    private String widgetType;

    private String title;
    private String description;
    private Long targetBoardId;
    private Integer postCount = 5;
    private Integer sortOrder = 0;
    private Boolean isActive = true;
    private String extraConfig;
    /** null = 홈 대시보드, non-null = 메뉴 대시보드 */
    private Long menuId;
}
