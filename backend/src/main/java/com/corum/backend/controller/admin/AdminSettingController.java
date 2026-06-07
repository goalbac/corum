package com.corum.backend.controller.admin;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.setting.SiteSettingResponse;
import com.corum.backend.dto.setting.SiteSettingUpdateRequest;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.setting.SiteSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/settings")
public class AdminSettingController {

    private final SiteSettingService siteSettingService;

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
}
