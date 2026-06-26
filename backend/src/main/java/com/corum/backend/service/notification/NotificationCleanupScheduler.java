package com.corum.backend.service.notification;

import com.corum.backend.domain.notification.NotificationRepository;
import com.corum.backend.domain.setting.SiteSetting;
import com.corum.backend.domain.setting.SiteSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupScheduler {

    private final NotificationRepository notificationRepository;
    private final SiteSettingRepository siteSettingRepository;

    /** 매일 새벽 3시: 보존 기간이 지난 읽은 알림 삭제 */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldNotifications() {
        int retentionDays = siteSettingRepository.findTopByOrderByIdAsc()
                .map(SiteSetting::getNotificationRetentionDays)
                .orElse(30);

        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        int deleted = notificationRepository.deleteReadOlderThan(cutoff);
        log.info("[알림 정리] {}일 이상 지난 읽은 알림 {}건 삭제 (기준: {})", retentionDays, deleted, cutoff);
    }
}
