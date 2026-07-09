package com.corum.backend.dto.setting;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class SiteSettingUpdateRequest {

    @NotBlank(message = "사이트명을 입력해주세요.")
    private String siteName;

    private String siteDescription;
    private String faviconUrl;

    @NotNull(message = "점검 모드 여부를 선택해주세요.")
    private Boolean maintenanceMode;

    private String maintenanceMessage;
    private LocalDateTime maintenanceUntil;

    @NotNull(message = "로그인 실패 제한을 입력해주세요.")
    @Min(value = 1, message = "로그인 실패 제한은 1 이상이어야 합니다.")
    private Integer loginFailLimit;

    @NotNull(message = "세션 타임아웃을 입력해주세요.")
    @Min(value = 1, message = "세션 타임아웃은 1분 이상이어야 합니다.")
    private Integer sessionTimeoutMin;

    @NotNull(message = "동시 로그인 허용 여부를 선택해주세요.")
    private Boolean allowConcurrentLogin;

    @NotNull(message = "파일 최대 용량을 입력해주세요.")
    @Min(value = 1, message = "파일 최대 용량은 1MB 이상이어야 합니다.")
    private Integer fileMaxSizeMb;

    private String fileAllowedExtensions;
    private String smtpHost;

    @NotNull(message = "SMTP 포트를 입력해주세요.")
    @Min(value = 1, message = "SMTP 포트는 1 이상이어야 합니다.")
    private Integer smtpPort;

    private String smtpUsername;
    private String smtpPasswordEnc;

    @NotNull(message = "SMTP TLS 사용 여부를 선택해주세요.")
    private Boolean smtpUseTls;

    private String footerHtml;
    private String contactAddress;
    private String contactPhone;
    private String adminEmail;

    @Min(value = 1, message = "알림 보존 기간은 1일 이상이어야 합니다.")
    private Integer notificationRetentionDays;

    private String defaultMenuAccessType;
    private List<Long> defaultMenuGroupIds;
    private Boolean requireLoginSiteWide;
}
