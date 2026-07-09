package com.corum.backend.domain.setting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "site_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class SiteSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_name", length = 200)
    private String siteName;

    @Column(name = "site_description", length = 500)
    private String siteDescription;

    @Column(name = "favicon_url", length = 500)
    private String faviconUrl;

    @Column(name = "maintenance_mode", nullable = false)
    private Boolean maintenanceMode;

    @Column(name = "maintenance_message", length = 500)
    private String maintenanceMessage;

    @Column(name = "maintenance_until")
    private LocalDateTime maintenanceUntil;

    @Column(name = "login_fail_limit", nullable = false)
    private Integer loginFailLimit;

    @Column(name = "session_timeout_min", nullable = false)
    private Integer sessionTimeoutMin;

    @Column(name = "allow_concurrent_login", nullable = false)
    private Boolean allowConcurrentLogin;

    @Column(name = "file_max_size_mb", nullable = false)
    private Integer fileMaxSizeMb;

    @Column(name = "file_allowed_extensions", length = 500)
    private String fileAllowedExtensions;

    @Column(name = "smtp_host", length = 200)
    private String smtpHost;

    @Column(name = "smtp_port", nullable = false)
    private Integer smtpPort;

    @Column(name = "smtp_username", length = 200)
    private String smtpUsername;

    @Column(name = "smtp_password_enc", length = 500)
    private String smtpPasswordEnc;

    @Column(name = "smtp_use_tls", nullable = false)
    private Boolean smtpUseTls;

    @Column(name = "footer_html", columnDefinition = "TEXT")
    private String footerHtml;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "contact_address", length = 500)
    private String contactAddress;

    @Column(name = "contact_phone", length = 100)
    private String contactPhone;

    @Column(name = "admin_email", length = 200)
    private String adminEmail;

    @Column(name = "notification_retention_days", nullable = false)
    @Builder.Default
    private Integer notificationRetentionDays = 30;

    // 관리자가 메뉴를 새로 만들 때 access_type 필드의 초기값으로 채워주는 용도(강제 아님, 편의 기본값)
    @Column(name = "default_menu_access_type", length = 20, nullable = false)
    @Builder.Default
    private String defaultMenuAccessType = "ALL";

    // defaultMenuAccessType=GROUP일 때 접근 허용 그룹의 초기값(콤마 구분 ID 목록, 예: "3,5")
    @Column(name = "default_menu_group_ids", length = 500)
    private String defaultMenuGroupIds;

    // 켜면 로그인/가입 등 게스트 전용 페이지를 제외한 사이트 전체가 비로그인 접근 시
    // 로그인 화면으로 리다이렉트된다 (프론트 라우터 가드에서 적용)
    @Column(name = "require_login_site_wide", nullable = false)
    @Builder.Default
    private Boolean requireLoginSiteWide = false;

    @Column(name = "vapid_public_key", columnDefinition = "TEXT")
    private String vapidPublicKey;

    @Column(name = "vapid_private_key", columnDefinition = "TEXT")
    private String vapidPrivateKey;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    public void update(
            String siteName,
            String siteDescription,
            String faviconUrl,
            Boolean maintenanceMode,
            String maintenanceMessage,
            LocalDateTime maintenanceUntil,
            Integer loginFailLimit,
            Integer sessionTimeoutMin,
            Boolean allowConcurrentLogin,
            Integer fileMaxSizeMb,
            String fileAllowedExtensions,
            String smtpHost,
            Integer smtpPort,
            String smtpUsername,
            String smtpPasswordEnc,
            Boolean smtpUseTls,
            String footerHtml,
            String contactAddress,
            String contactPhone,
            String adminEmail,
            Integer notificationRetentionDays,
            String defaultMenuAccessType,
            String defaultMenuGroupIds,
            Boolean requireLoginSiteWide,
            Long updatedBy
    ) {
        this.siteName = siteName;
        this.siteDescription = siteDescription;
        this.faviconUrl = faviconUrl;
        this.maintenanceMode = maintenanceMode;
        this.maintenanceMessage = maintenanceMessage;
        this.maintenanceUntil = maintenanceUntil;
        this.loginFailLimit = loginFailLimit;
        this.sessionTimeoutMin = sessionTimeoutMin;
        this.allowConcurrentLogin = allowConcurrentLogin;
        this.fileMaxSizeMb = fileMaxSizeMb;
        this.fileAllowedExtensions = fileAllowedExtensions;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.smtpUsername = smtpUsername;
        this.smtpPasswordEnc = smtpPasswordEnc;
        this.smtpUseTls = smtpUseTls;
        this.footerHtml = footerHtml;
        this.contactAddress = contactAddress;
        this.contactPhone = contactPhone;
        this.adminEmail = adminEmail;
        this.notificationRetentionDays = notificationRetentionDays != null ? notificationRetentionDays : 30;
        this.defaultMenuAccessType = defaultMenuAccessType != null ? defaultMenuAccessType : "ALL";
        this.defaultMenuGroupIds = defaultMenuGroupIds;
        this.requireLoginSiteWide = requireLoginSiteWide != null ? requireLoginSiteWide : false;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateVapidKeys(String publicKey, String privateKey) {
        this.vapidPublicKey = publicKey;
        this.vapidPrivateKey = privateKey;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateFaviconUrl(String faviconUrl) {
        this.faviconUrl = faviconUrl;
        this.updatedAt = LocalDateTime.now();
    }
}
