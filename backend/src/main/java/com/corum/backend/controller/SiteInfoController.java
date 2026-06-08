package com.corum.backend.controller;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.domain.setting.SiteSetting;
import com.corum.backend.domain.setting.SiteSettingRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/site")
public class SiteInfoController {

    private final SiteSettingRepository siteSettingRepository;

    @GetMapping("/public")
    public ApiResponse<SitePublicInfo> getPublicInfo() {
        SiteSetting s = siteSettingRepository.findTopByOrderByIdAsc().orElse(null);
        if (s == null) return ApiResponse.ok(new SitePublicInfo(null, false, null, null, null));
        boolean inMaintenance = Boolean.TRUE.equals(s.getMaintenanceMode()) &&
                (s.getMaintenanceUntil() == null || s.getMaintenanceUntil().isAfter(LocalDateTime.now()));
        return ApiResponse.ok(new SitePublicInfo(s.getSiteName(), inMaintenance,
                s.getMaintenanceMessage(), s.getMaintenanceUntil(), s.getFooterHtml()));
    }

    @Getter
    public static class SitePublicInfo {
        private final String siteName;
        private final boolean maintenanceMode;
        private final String maintenanceMessage;
        private final LocalDateTime maintenanceUntil;
        private final String footerHtml;

        public SitePublicInfo(String siteName, boolean maintenanceMode,
                              String maintenanceMessage, LocalDateTime maintenanceUntil, String footerHtml) {
            this.siteName = siteName;
            this.maintenanceMode = maintenanceMode;
            this.maintenanceMessage = maintenanceMessage;
            this.maintenanceUntil = maintenanceUntil;
            this.footerHtml = footerHtml;
        }
    }
}
