package com.corum.backend.service.notification;

import com.corum.backend.domain.notification.PushSubscription;
import com.corum.backend.domain.notification.PushSubscriptionRepository;
import com.corum.backend.domain.setting.SiteSetting;
import com.corum.backend.domain.setting.SiteSettingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;

import java.util.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebPushService {

    private final SiteSettingRepository siteSettingRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void registerProvider() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    // ===== VAPID 키 생성 (관리자 요청 시) =====
    @Transactional
    public String generateVapidKeys() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("EC", "BC");
            gen.initialize(new ECGenParameterSpec("prime256v1"));
            KeyPair keyPair = gen.generateKeyPair();
            Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
            String pubKey  = enc.encodeToString(Utils.encode((ECPublicKey)  keyPair.getPublic()));
            String privKey = enc.encodeToString(Utils.encode((ECPrivateKey) keyPair.getPrivate()));

            siteSettingRepository.findTopByOrderByIdAsc().ifPresent(s -> s.updateVapidKeys(pubKey, privKey));
            return pubKey;
        } catch (Exception e) {
            log.error("VAPID 키 생성 실패", e);
            throw new RuntimeException("VAPID 키 생성에 실패했습니다: " + e.getMessage());
        }
    }

    // ===== VAPID 공개키 조회 =====
    public String getVapidPublicKey() {
        return siteSettingRepository.findTopByOrderByIdAsc()
                .map(SiteSetting::getVapidPublicKey)
                .orElse(null);
    }

    // ===== 구독 저장 =====
    @Transactional
    public void subscribe(Long memberId, String endpoint, String p256dh, String authKey, String userAgent) {
        if (pushSubscriptionRepository.existsByMemberIdAndEndpoint(memberId, endpoint)) return;
        pushSubscriptionRepository.save(PushSubscription.builder()
                .memberId(memberId)
                .endpoint(endpoint)
                .p256dh(p256dh)
                .authKey(authKey)
                .userAgent(userAgent)
                .build());
    }

    // ===== 구독 취소 =====
    @Transactional
    public void unsubscribe(Long memberId, String endpoint) {
        pushSubscriptionRepository.findByEndpoint(endpoint).ifPresent(sub -> {
            if (sub.getMemberId().equals(memberId)) {
                pushSubscriptionRepository.delete(sub);
            }
        });
    }

    // ===== 푸시 발송 (비동기) =====
    @Async
    public void sendPush(Long memberId, String title, String content, String linkUrl) {
        SiteSetting setting = siteSettingRepository.findTopByOrderByIdAsc().orElse(null);
        if (setting == null || setting.getVapidPublicKey() == null || setting.getVapidPrivateKey() == null) {
            return;
        }

        List<PushSubscription> subscriptions = pushSubscriptionRepository.findByMemberId(memberId);
        if (subscriptions.isEmpty()) return;

        PushService pushService;
        try {
            pushService = new PushService(
                    setting.getVapidPublicKey(),
                    setting.getVapidPrivateKey(),
                    "mailto:admin@corum.app"
            );
        } catch (Exception e) {
            log.warn("PushService 초기화 실패: {}", e.getMessage());
            return;
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                    "title",   title != null ? title : "",
                    "content", content != null ? content : "",
                    "linkUrl", linkUrl != null ? linkUrl : "/"
            ));
        } catch (Exception e) {
            return;
        }

        for (PushSubscription sub : subscriptions) {
            try {
                Notification notif = new Notification(sub.getEndpoint(), sub.getP256dh(), sub.getAuthKey(), payload);
                pushService.send(notif);
            } catch (Exception e) {
                // 만료된 구독 정리
                log.debug("푸시 발송 실패 ({}), 구독 삭제: {}", e.getMessage(), sub.getEndpoint());
                pushSubscriptionRepository.deleteByEndpoint(sub.getEndpoint());
            }
        }
    }
}
