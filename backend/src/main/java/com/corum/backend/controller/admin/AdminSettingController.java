package com.corum.backend.controller.admin;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.setting.SiteSettingResponse;
import com.corum.backend.dto.setting.SiteSettingUpdateRequest;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.notification.WebPushService;
import com.corum.backend.service.setting.SiteSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/settings")
public class AdminSettingController {

    private final SiteSettingService siteSettingService;
    private final WebPushService webPushService;

    @GetMapping
    public ApiResponse<SiteSettingResponse> getSetting() {
        return ApiResponse.ok(siteSettingService.getSetting());
    }

    @PutMapping
    public ApiResponse<SiteSettingResponse> updateSetting(
            @Valid @RequestBody SiteSettingUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long updatedBy = userDetails != null ? userDetails.getMemberId() : null;
        return ApiResponse.ok("사이트 설정이 저장되었습니다.", siteSettingService.updateSetting(request, updatedBy));
    }

    @PostMapping("/logo")
    public ApiResponse<SiteSettingResponse> uploadLogo(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long updatedBy = userDetails != null ? userDetails.getMemberId() : null;
        return ApiResponse.ok("로고가 업로드되었습니다.", siteSettingService.uploadLogo(file, updatedBy));
    }

    @PostMapping("/favicon")
    public ApiResponse<SiteSettingResponse> uploadFavicon(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long updatedBy = userDetails != null ? userDetails.getMemberId() : null;
        return ApiResponse.ok("파비콘이 업로드되었습니다.", siteSettingService.uploadFavicon(file, updatedBy));
    }

    // VAPID 키 생성 (관리자)
    @PostMapping("/vapid/generate")
    public ApiResponse<Map<String, String>> generateVapid() {
        String pubKey = webPushService.generateVapidKeys();
        return ApiResponse.ok(Map.of("vapidPublicKey", pubKey));
    }
}
