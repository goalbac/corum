package com.corum.backend.controller.admin;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.dashboard.DashboardInfoResponse;
import com.corum.backend.dto.dashboard.DashboardWidgetRequest;
import com.corum.backend.dto.dashboard.DashboardWidgetResponse;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.dashboard.DashboardWidgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardWidgetController {

    private final DashboardWidgetService dashboardWidgetService;

    @GetMapping("/list")
    public ApiResponse<List<DashboardInfoResponse>> getDashboardList() {
        return ApiResponse.ok(dashboardWidgetService.getDashboardList());
    }

    @GetMapping("/widgets")
    public ApiResponse<List<DashboardWidgetResponse>> getWidgets(
            @RequestParam(required = false) Long menuId) {
        return ApiResponse.ok(dashboardWidgetService.getAdminWidgets(menuId));
    }

    @PostMapping("/widgets")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DashboardWidgetResponse> create(
            @Valid @RequestBody DashboardWidgetRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        return ApiResponse.ok(dashboardWidgetService.create(request, memberId));
    }

    @PutMapping("/widgets/sort")
    public ApiResponse<Void> updateSortOrder(
            @RequestBody List<Long> widgetIds,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        dashboardWidgetService.updateSortOrder(widgetIds, memberId);
        return ApiResponse.ok("?쒖꽌媛 蹂寃쎈릺?덉뒿?덈떎.");
    }

    @PutMapping("/widgets/{id}")
    public ApiResponse<DashboardWidgetResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DashboardWidgetRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        return ApiResponse.ok(dashboardWidgetService.update(id, request, memberId));
    }

    @DeleteMapping("/widgets/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        dashboardWidgetService.delete(id);
        return ApiResponse.ok("대시보드 위젯이 삭제되었습니다.");
    }
}
