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
