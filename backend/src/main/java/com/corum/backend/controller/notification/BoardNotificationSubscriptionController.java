package com.corum.backend.controller.notification;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.notification.BoardNotificationSubscriptionRequest;
import com.corum.backend.dto.notification.BoardNotificationSubscriptionResponse;
import com.corum.backend.dto.notification.MyBoardSubscriptionResponse;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.notification.BoardNotificationSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BoardNotificationSubscriptionController {

    private final BoardNotificationSubscriptionService subscriptionService;

    @GetMapping("/api/boards/{boardId}/notification-subscription")
    public ApiResponse<BoardNotificationSubscriptionResponse> getSubscription(
            @PathVariable Long boardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(subscriptionService.getSubscription(userDetails.getMemberId(), boardId));
    }

    @PutMapping("/api/boards/{boardId}/notification-subscription")
    public ApiResponse<BoardNotificationSubscriptionResponse> updateSubscription(
            @PathVariable Long boardId,
            @RequestBody BoardNotificationSubscriptionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(subscriptionService.updateSubscription(
                userDetails.getMemberId(), boardId, request.getNotifyNewPost(), request.getNotifyNewComment()));
    }

    @GetMapping("/api/notifications/board-subscriptions")
    public ApiResponse<List<MyBoardSubscriptionResponse>> getMySubscriptions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(subscriptionService.getMySubscriptions(userDetails.getMemberId()));
    }
}
