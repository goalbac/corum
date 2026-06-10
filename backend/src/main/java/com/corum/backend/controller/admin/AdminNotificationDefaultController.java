package com.corum.backend.controller.admin;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.domain.notification.NotificationDefault;
import com.corum.backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/notification-defaults")
@RequiredArgsConstructor
public class AdminNotificationDefaultController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<NotificationDefault>> getDefaults() {
        return ApiResponse.ok(notificationService.getDefaults());
    }

    @PutMapping
    public ApiResponse<Void> updateDefaults(@RequestBody Map<String, Boolean> updates) {
        notificationService.updateDefaults(updates);
        return ApiResponse.ok("저장되었습니다.");
    }
}
