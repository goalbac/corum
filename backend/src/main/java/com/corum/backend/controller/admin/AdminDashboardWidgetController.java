package com.corum.backend.controller.admin;

import com.corum.backend.common.ApiResponse;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard/widgets")
public class AdminDashboardWidgetController {

    private final DashboardWidgetService dashboardWidgetService;

    @GetMapping
    public ApiResponse<List<DashboardWidgetResponse>> getWidgets() {
        return ApiResponse.ok(dashboardWidgetService.getAdminWidgets());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DashboardWidgetResponse> create(
            @Valid @RequestBody DashboardWidgetRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        return ApiResponse.ok(dashboardWidgetService.create(request, memberId));
    }

    @PutMapping("/sort")
    public ApiResponse<Void> updateSortOrder(
            @RequestBody List<Long> widgetIds,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        dashboardWidgetService.updateSortOrder(widgetIds, memberId);
        return ApiResponse.ok("?쒖꽌媛 蹂寃쎈릺?덉뒿?덈떎.");
    }

    @PutMapping("/{id}")
    public ApiResponse<DashboardWidgetResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DashboardWidgetRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        return ApiResponse.ok(dashboardWidgetService.update(id, request, memberId));
    }

    @PutMapping("/sort")
    public ApiResponse<Void> reorder(@RequestBody List<Long> orderedIds) {
        dashboardWidgetService.reorder(orderedIds);
        return ApiResponse.ok("순서가 저장되었습니다.");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        dashboardWidgetService.delete(id);
        return ApiResponse.ok("대시보드 위젯이 삭제되었습니다.");
    }
}
