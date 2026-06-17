package com.corum.backend.controller.admin;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.stats.DailyStatsResponse;
import com.corum.backend.service.stats.StatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    private final StatService statService;

    @GetMapping("/daily")
    public ApiResponse<DailyStatsResponse> getDaily(
            @RequestParam(defaultValue = "14") int days) {
        int clampedDays = Math.min(Math.max(days, 1), 90);
        return ApiResponse.ok(statService.getDailyStats(clampedDays));
    }
}
