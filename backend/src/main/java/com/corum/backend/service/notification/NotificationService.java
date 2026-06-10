package com.corum.backend.service.notification;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.notification.*;
import com.corum.backend.dto.notification.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final MemberNotificationPrefRepository prefRepository;
    private final NotificationDefaultRepository defaultRepository;

    // ===== 알림 생성 (수신 설정 체크) =====
    @Transactional
    public void create(Long memberId, String type, String title, String content, String linkUrl) {
        if (!isEnabled(memberId, type)) return;
        Notification notification = Notification.builder()
                .memberId(memberId)
                .type(type)
                .title(title)
                .content(content)
                .linkUrl(linkUrl)
                .build();
        Notification saved = notificationRepository.save(notification);
        sseEmitterRegistry.send(memberId, new NotificationResponse(saved));
    }

    @Transactional
    public void createForMembers(List<Long> memberIds, String type, String title, String content, String linkUrl) {
        for (Long memberId : memberIds) {
            create(memberId, type, title, content, linkUrl);
        }
    }

    // ===== 수신 설정 체크 =====
    private boolean isEnabled(Long memberId, String type) {
        return prefRepository.findEnabledByMemberIdAndNotifType(memberId, type)
                .orElseGet(() -> defaultRepository.findById(type).map(NotificationDefault::getEnabled).orElse(true));
    }

    // ===== 회원 가입 시 기본 수신 설정 초기화 =====
    @Transactional
    public void initPrefsForNewMember(Long memberId) {
        List<NotificationDefault> defaults = defaultRepository.findAll();
        List<MemberNotificationPref> prefs = defaults.stream()
                .map(d -> MemberNotificationPref.builder()
                        .memberId(memberId)
                        .notifType(d.getNotifType())
                        .enabled(d.getEnabled())
                        .build())
                .collect(Collectors.toList());
        prefRepository.saveAll(prefs);
    }

    // ===== 내 수신 설정 조회 =====
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPrefs(Long memberId) {
        List<NotificationDefault> allTypes = defaultRepository.findAllByOrderByNotifTypeAsc();
        Map<String, Boolean> myPrefs = prefRepository.findByMemberId(memberId).stream()
                .collect(Collectors.toMap(MemberNotificationPref::getNotifType, MemberNotificationPref::getEnabled));

        return allTypes.stream().map(d -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("notifType", d.getNotifType());
            row.put("label",     d.getLabel());
            row.put("enabled",   myPrefs.getOrDefault(d.getNotifType(), d.getEnabled()));
            return row;
        }).collect(Collectors.toList());
    }

    // ===== 내 수신 설정 업데이트 =====
    @Transactional
    public void updatePrefs(Long memberId, Map<String, Boolean> updates) {
        updates.forEach((type, enabled) -> {
            MemberNotificationPref pref = prefRepository.findByMemberIdAndNotifType(memberId, type)
                    .orElseGet(() -> MemberNotificationPref.builder()
                            .memberId(memberId).notifType(type).build());
            pref.setEnabled(enabled);
            prefRepository.save(pref);
        });
    }

    // ===== 알림 기본값 조회 (관리자) =====
    @Transactional(readOnly = true)
    public List<NotificationDefault> getDefaults() {
        return defaultRepository.findAllByOrderByNotifTypeAsc();
    }

    // ===== 알림 기본값 업데이트 (관리자) =====
    @Transactional
    public void updateDefaults(Map<String, Boolean> updates) {
        updates.forEach((type, enabled) -> {
            defaultRepository.findById(type).ifPresent(d -> {
                d.setEnabled(enabled);
                defaultRepository.save(d);
            });
        });
    }

    // ===== 이하 기존 API =====

    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecent(Long memberId) {
        return notificationRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream().limit(30).map(NotificationResponse::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long memberId) {
        return notificationRepository.countByMemberIdAndIsReadFalse(memberId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long memberId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> BusinessException.notFound("알림을 찾을 수 없습니다."));
        if (!notification.getMemberId().equals(memberId))
            throw BusinessException.forbidden("접근 권한이 없습니다.");
        notification.markAsRead();
    }

    @Transactional
    public void markAllAsRead(Long memberId) {
        notificationRepository.markAllReadByMemberId(memberId);
    }

    @Transactional
    public void delete(Long notificationId, Long memberId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> BusinessException.notFound("알림을 찾을 수 없습니다."));
        if (!notification.getMemberId().equals(memberId))
            throw BusinessException.forbidden("접근 권한이 없습니다.");
        notificationRepository.delete(notification);
    }

    @Transactional
    public void deleteAll(Long memberId) {
        notificationRepository.deleteAllByMemberId(memberId);
    }
}
