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
        if (s == null) return ApiResponse.ok(new SitePublicInfo(
                null, null, null, null, false, null, null, null, null, null, null));
        boolean inMaintenance = Boolean.TRUE.equals(s.getMaintenanceMode()) &&
                (s.getMaintenanceUntil() == null || s.getMaintenanceUntil().isAfter(LocalDateTime.now()));
        return ApiResponse.ok(new SitePublicInfo(
                s.getSiteName(),
                s.getSiteDescription(),
                s.getLogoUrl(),
                s.getFaviconUrl(),
                inMaintenance,
                s.getMaintenanceMessage(),
                s.getMaintenanceUntil(),
                s.getFooterHtml(),
                s.getContactAddress(),
                s.getContactPhone(),
                s.getAdminEmail()
        ));
    }

    @Getter
    public static class SitePublicInfo {
        private final String siteName;
        private final String siteDescription;
        private final String logoUrl;
        private final String faviconUrl;
        private final boolean maintenanceMode;
        private final String maintenanceMessage;
        private final LocalDateTime maintenanceUntil;
        private final String footerHtml;
        private final String contactAddress;
        private final String contactPhone;
        private final String adminEmail;

        public SitePublicInfo(String siteName, String siteDescription, String logoUrl, String faviconUrl,
                              boolean maintenanceMode, String maintenanceMessage,
                              LocalDateTime maintenanceUntil, String footerHtml,
                              String contactAddress, String contactPhone, String adminEmail) {
            this.siteName = siteName;
            this.siteDescription = siteDescription;
            this.logoUrl = logoUrl;
            this.faviconUrl = faviconUrl;
            this.maintenanceMode = maintenanceMode;
            this.maintenanceMessage = maintenanceMessage;
            this.maintenanceUntil = maintenanceUntil;
            this.footerHtml = footerHtml;
            this.contactAddress = contactAddress;
            this.contactPhone = contactPhone;
            this.adminEmail = adminEmail;
        }
    }
}
