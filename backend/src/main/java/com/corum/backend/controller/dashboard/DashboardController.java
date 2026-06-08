package com.corum.backend.controller.dashboard;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.dashboard.DashboardStatsResponse;
import com.corum.backend.dto.dashboard.DashboardWidgetResponse;
import com.corum.backend.service.dashboard.DashboardWidgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardWidgetService dashboardWidgetService;

    @GetMapping("/widgets")
    public ApiResponse<List<DashboardWidgetResponse>> getWidgets() {
        return ApiResponse.ok(dashboardWidgetService.getActiveWidgets());
    }

    @GetMapping("/stats")
    public ApiResponse<DashboardStatsResponse> getStats() {
        return ApiResponse.ok(dashboardWidgetService.getStats());
    }
}
