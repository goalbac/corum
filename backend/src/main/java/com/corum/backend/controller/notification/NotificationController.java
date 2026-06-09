package com.corum.backend.controller.notification;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.notification.NotificationResponse;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.notification.NotificationService;
import com.corum.backend.service.notification.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterRegistry sseEmitterRegistry;

    // SSE 스트림 구독 (token 쿼리파라미터로 인증)
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return sseEmitterRegistry.subscribe(userDetails.getMemberId());
    }

    // 알림 목록
    @GetMapping
    public ApiResponse<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(notificationService.getRecent(userDetails.getMemberId()));
    }

    // 안 읽은 알림 수
    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        long count = notificationService.getUnreadCount(userDetails.getMemberId());
        return ApiResponse.ok(Map.of("count", count));
    }

    // 단건 읽음 처리
    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAsRead(id, userDetails.getMemberId());
        return ApiResponse.ok("읽음 처리되었습니다.");
    }

    // 전체 읽음 처리
    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getMemberId());
        return ApiResponse.ok("전체 읽음 처리되었습니다.");
    }
}
