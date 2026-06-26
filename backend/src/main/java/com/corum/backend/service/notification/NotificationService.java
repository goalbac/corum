package com.corum.backend.service.notification;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.member.MemberRepository;
import com.corum.backend.domain.notification.*;
import com.corum.backend.dto.notification.NotificationResponse;
import com.corum.backend.service.mail.MailService;
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
    private final MemberRepository memberRepository;
    private final MailService mailService;
    private final WebPushService webPushService;

    // ===== 알림 생성 =====
    @Transactional
    public void create(Long memberId, String type, String title, String content, String linkUrl) {
        boolean sysOn   = isSystemEnabled(memberId, type);
        boolean emailOn = isEmailEnabled(memberId, type);

        if (sysOn) {
            Notification notification = Notification.builder()
                    .memberId(memberId)
                    .type(type)
                    .title(title)
                    .content(content)
                    .linkUrl(linkUrl)
                    .build();
            Notification saved = notificationRepository.save(notification);
            sseEmitterRegistry.send(memberId, new NotificationResponse(saved));
            // Web Push: 탭이 닫혀 있어도 수신 가능
            webPushService.sendPush(memberId, title, content, linkUrl);
        }

        if (emailOn) {
            sendEmailNotif(memberId, title, content, linkUrl);
        }
    }

    @Transactional
    public void createForMembers(List<Long> memberIds, String type, String title, String content, String linkUrl) {
        for (Long memberId : memberIds) {
            create(memberId, type, title, content, linkUrl);
        }
    }

    // ===== 채널별 수신 여부 =====
    private boolean isSystemEnabled(Long memberId, String type) {
        return prefRepository.findPref(memberId, type)
                .map(MemberNotificationPref::getSystemEnabled)
                .orElseGet(() -> defaultRepository.findById(type)
                        .map(NotificationDefault::getSystemEnabled).orElse(true));
    }

    private boolean isEmailEnabled(Long memberId, String type) {
        return prefRepository.findPref(memberId, type)
                .map(MemberNotificationPref::getEmailEnabled)
                .orElseGet(() -> defaultRepository.findById(type)
                        .map(NotificationDefault::getEmailEnabled).orElse(false));
    }

    // ===== 이메일 알림 발송 =====
    private void sendEmailNotif(Long memberId, String title, String content, String linkUrl) {
        try {
            memberRepository.findById(memberId).ifPresent(member -> {
                if (member.getEmail() == null) return;
                String body = "<p><b>" + title + "</b></p>"
                        + (content != null ? "<p>" + content + "</p>" : "")
                        + (linkUrl != null ? "<p><a href='http://localhost:5173" + linkUrl + "'>바로가기</a></p>" : "");
                mailService.sendAsync(memberId, member.getEmail(), "[Corum] " + title, body, "NOTIFICATION");
            });
        } catch (Exception ignored) { }
    }

    // ===== 회원 가입 시 기본 수신 설정 초기화 =====
    @Transactional
    public void initPrefsForNewMember(Long memberId) {
        List<NotificationDefault> defaults = defaultRepository.findAll();
        List<MemberNotificationPref> prefs = defaults.stream()
                .map(d -> MemberNotificationPref.builder()
                        .memberId(memberId)
                        .notifType(d.getNotifType())
                        .systemEnabled(d.getSystemEnabled())
                        .emailEnabled(d.getEmailEnabled())
                        .build())
                .collect(Collectors.toList());
        prefRepository.saveAll(prefs);
    }

    // ===== 내 수신 설정 조회 =====
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPrefs(Long memberId) {
        List<NotificationDefault> allTypes = defaultRepository.findAllByOrderByNotifTypeAsc();
        Map<String, MemberNotificationPref> myPrefs = prefRepository.findByMemberId(memberId).stream()
                .collect(Collectors.toMap(MemberNotificationPref::getNotifType, p -> p));

        return allTypes.stream().map(d -> {
            MemberNotificationPref p = myPrefs.get(d.getNotifType());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("notifType",      d.getNotifType());
            row.put("label",          d.getLabel());
            row.put("systemEnabled",  p != null ? p.getSystemEnabled() : d.getSystemEnabled());
            row.put("emailEnabled",   p != null ? p.getEmailEnabled()  : d.getEmailEnabled());
            return row;
        }).collect(Collectors.toList());
    }

    // ===== 내 수신 설정 업데이트 =====
    // payload: { "notifType": { "system": true, "email": false }, ... }
    @Transactional
    public void updatePrefs(Long memberId, Map<String, Map<String, Boolean>> updates) {
        updates.forEach((type, channels) -> {
            MemberNotificationPref pref = prefRepository.findByMemberIdAndNotifType(memberId, type)
                    .orElseGet(() -> MemberNotificationPref.builder()
                            .memberId(memberId).notifType(type).build());
            if (channels.containsKey("system")) pref.setSystemEnabled(channels.get("system"));
            if (channels.containsKey("email"))  pref.setEmailEnabled(channels.get("email"));
            prefRepository.save(pref);
        });
    }

    // ===== 알림 기본값 조회 (관리자) =====
    @Transactional(readOnly = true)
    public List<NotificationDefault> getDefaults() {
        return defaultRepository.findAllByOrderByNotifTypeAsc();
    }

    // ===== 알림 기본값 업데이트 (관리자) =====
    // payload: { "notifType": { "system": true, "email": false }, ... }
    @Transactional
    public void updateDefaults(Map<String, Map<String, Boolean>> updates) {
        updates.forEach((type, channels) -> defaultRepository.findById(type).ifPresent(d -> {
            if (channels.containsKey("system")) d.setSystemEnabled(channels.get("system"));
            if (channels.containsKey("email"))  d.setEmailEnabled(channels.get("email"));
            defaultRepository.save(d);
        }));
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
