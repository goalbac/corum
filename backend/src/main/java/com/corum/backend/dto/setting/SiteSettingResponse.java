package com.corum.backend.dto.setting;

import com.corum.backend.domain.setting.SiteSetting;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SiteSettingResponse {

    private final Long id;
    private final String siteName;
    private final String siteDescription;
    private final String faviconUrl;
    private final Boolean maintenanceMode;
    private final String maintenanceMessage;
    private final LocalDateTime maintenanceUntil;
    private final Integer loginFailLimit;
    private final Integer sessionTimeoutMin;
    private final Boolean allowConcurrentLogin;
    private final Integer fileMaxSizeMb;
    private final String fileAllowedExtensions;
    private final String smtpHost;
    private final Integer smtpPort;
    private final String smtpUsername;
    private final String smtpPasswordEnc;
    private final Boolean smtpUseTls;
    private final String footerHtml;
    private final String logoUrl;
    private final String contactAddress;
    private final String contactPhone;
    private final String adminEmail;
    private final Integer notificationRetentionDays;
    private final String defaultMenuAccessType;
    private final Boolean requireLoginSiteWide;
    private final String vapidPublicKey;
    private final LocalDateTime updatedAt;
    private final Long updatedBy;

    public SiteSettingResponse(SiteSetting setting) {
        this.id = setting.getId();
        this.siteName = setting.getSiteName();
        this.siteDescription = setting.getSiteDescription();
        this.faviconUrl = setting.getFaviconUrl();
        this.maintenanceMode = setting.getMaintenanceMode();
        this.maintenanceMessage = setting.getMaintenanceMessage();
        this.maintenanceUntil = setting.getMaintenanceUntil();
        this.loginFailLimit = setting.getLoginFailLimit();
        this.sessionTimeoutMin = setting.getSessionTimeoutMin();
        this.allowConcurrentLogin = setting.getAllowConcurrentLogin();
        this.fileMaxSizeMb = setting.getFileMaxSizeMb();
        this.fileAllowedExtensions = setting.getFileAllowedExtensions();
        this.smtpHost = setting.getSmtpHost();
        this.smtpPort = setting.getSmtpPort();
        this.smtpUsername = setting.getSmtpUsername();
        this.smtpPasswordEnc = setting.getSmtpPasswordEnc();
        this.smtpUseTls = setting.getSmtpUseTls();
        this.footerHtml = setting.getFooterHtml();
        this.logoUrl = setting.getLogoUrl();
        this.contactAddress = setting.getContactAddress();
        this.contactPhone = setting.getContactPhone();
        this.adminEmail = setting.getAdminEmail();
        this.notificationRetentionDays = setting.getNotificationRetentionDays();
        this.defaultMenuAccessType = setting.getDefaultMenuAccessType();
        this.requireLoginSiteWide = setting.getRequireLoginSiteWide();
        this.vapidPublicKey = setting.getVapidPublicKey();
        this.updatedAt = setting.getUpdatedAt();
        this.updatedBy = setting.getUpdatedBy();
    }
}
