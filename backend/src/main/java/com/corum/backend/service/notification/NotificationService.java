package com.corum.backend.service.notification;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.notification.Notification;
import com.corum.backend.domain.notification.NotificationRepository;
import com.corum.backend.dto.notification.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseEmitterRegistry sseEmitterRegistry;

    @Transactional
    public void create(Long memberId, String type, String title, String content, String linkUrl) {
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

    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecent(Long memberId) {
        return notificationRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .limit(30)
                .map(NotificationResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long memberId) {
        return notificationRepository.countByMemberIdAndIsReadFalse(memberId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long memberId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> BusinessException.notFound("알림을 찾을 수 없습니다."));
        if (!notification.getMemberId().equals(memberId)) {
            throw BusinessException.forbidden("접근 권한이 없습니다.");
        }
        notification.markAsRead();
    }

    @Transactional
    public void markAllAsRead(Long memberId) {
        notificationRepository.markAllReadByMemberId(memberId);
    }
}
