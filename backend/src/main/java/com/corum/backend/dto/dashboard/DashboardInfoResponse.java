package com.corum.backend.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardInfoResponse {
    private final Long menuId;       // null = 홈 대시보드
    private final String menuPath;   // "홈 대시보드" or "메뉴1 > 메뉴2"
    private final String menuName;   // 마지막 메뉴명
    private final int widgetCount;
    @JsonProperty("isHome")
    private final boolean isHome;
}
