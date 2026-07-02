package com.corum.backend.service.auth;

import com.corum.backend.domain.auth.InvalidatedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvalidatedTokenCleanupScheduler {

    private final InvalidatedTokenRepository invalidatedTokenRepository;

    /** 매일 새벽 4시: 만료 시각이 지난 무효화 토큰 기록 삭제 */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = invalidatedTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("[토큰 블랙리스트 정리] 만료된 무효화 토큰 {}건 삭제", deleted);
    }
}
