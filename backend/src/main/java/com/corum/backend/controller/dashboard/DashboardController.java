package com.corum.backend.controller.dashboard;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.dashboard.DashboardCalendarEventResponse;
import com.corum.backend.dto.dashboard.DashboardStatsResponse;
import com.corum.backend.dto.dashboard.DashboardWidgetResponse;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.dashboard.DashboardWidgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardWidgetService dashboardWidgetService;

    /** 전체 위젯 데이터 (기존 호환용 — 사용 지양) */
    @GetMapping("/widgets")
    public ApiResponse<List<DashboardWidgetResponse>> getWidgets(
            @RequestParam(required = false) Long menuId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        return ApiResponse.ok(dashboardWidgetService.getActiveWidgets(menuId, memberId));
    }

    /** 위젯 레이아웃만 (메타데이터, 데이터 없음 — 빠름) */
    @GetMapping("/widgets/layout")
    public ApiResponse<List<DashboardWidgetResponse>> getWidgetLayouts(
            @RequestParam(required = false) Long menuId) {
        return ApiResponse.ok(dashboardWidgetService.getActiveWidgetLayouts(menuId));
    }

    /** 단일 위젯 데이터 로드 */
    @GetMapping("/widgets/{id}")
    public ApiResponse<DashboardWidgetResponse> getWidgetData(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int weekOffset,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        return ApiResponse.ok(dashboardWidgetService.getWidgetData(id, weekOffset, memberId));
    }

    @GetMapping("/stats")
    public ApiResponse<DashboardStatsResponse> getStats() {
        return ApiResponse.ok(dashboardWidgetService.getStats());
    }

    /** 이번 주 일정 (홈 사이드바 '주간 스트립' 위젯용, 열람 권한 필터링됨) */
    @GetMapping("/week-events")
    public ApiResponse<List<DashboardCalendarEventResponse>> getWeekEvents(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        return ApiResponse.ok(dashboardWidgetService.getWeekEvents(memberId));
    }
}
