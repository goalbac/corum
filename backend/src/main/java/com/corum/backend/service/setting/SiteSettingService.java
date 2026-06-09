package com.corum.backend.service.setting;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.setting.SiteSetting;
import com.corum.backend.domain.setting.SiteSettingRepository;
import com.corum.backend.dto.setting.SiteSettingResponse;
import com.corum.backend.dto.setting.SiteSettingUpdateRequest;
import com.corum.backend.service.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SiteSettingService {

    private final SiteSettingRepository siteSettingRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public SiteSettingResponse getSetting() {
        SiteSetting setting = siteSettingRepository.findTopByOrderByIdAsc()
                .orElseThrow(() -> BusinessException.notFound("사이트 설정을 찾을 수 없습니다."));
        return new SiteSettingResponse(setting);
    }

    @Transactional
    public SiteSettingResponse updateSetting(SiteSettingUpdateRequest request, Long updatedBy) {
        SiteSetting setting = siteSettingRepository.findTopByOrderByIdAsc()
                .orElseThrow(() -> BusinessException.notFound("사이트 설정을 찾을 수 없습니다."));
        setting.update(
                request.getSiteName(),
                request.getSiteDescription(),
                request.getFaviconUrl(),
                request.getMaintenanceMode(),
                request.getMaintenanceMessage(),
                request.getMaintenanceUntil(),
                request.getLoginFailLimit(),
                request.getSessionTimeoutMin(),
                request.getAllowConcurrentLogin(),
                request.getFileMaxSizeMb(),
                request.getFileAllowedExtensions(),
                request.getSmtpHost(),
                request.getSmtpPort(),
                request.getSmtpUsername(),
                request.getSmtpPasswordEnc(),
                request.getSmtpUseTls(),
                request.getFooterHtml(),
                request.getContactAddress(),
                request.getContactPhone(),
                request.getAdminEmail(),
                updatedBy
        );
        return new SiteSettingResponse(setting);
    }

    @Transactional
    public SiteSettingResponse uploadLogo(MultipartFile file, Long updatedBy) {
        SiteSetting setting = siteSettingRepository.findTopByOrderByIdAsc()
                .orElseThrow(() -> BusinessException.notFound("사이트 설정을 찾을 수 없습니다."));
        String url = fileStorageService.uploadSiteAsset("logo", file);
        setting.updateLogoUrl(url);
        return new SiteSettingResponse(setting);
    }

    @Transactional
    public SiteSettingResponse uploadFavicon(MultipartFile file, Long updatedBy) {
        SiteSetting setting = siteSettingRepository.findTopByOrderByIdAsc()
                .orElseThrow(() -> BusinessException.notFound("사이트 설정을 찾을 수 없습니다."));
        String url = fileStorageService.uploadSiteAsset("favicon", file);
        setting.updateFaviconUrl(url);
        return new SiteSettingResponse(setting);
    }
}
