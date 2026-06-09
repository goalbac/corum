package com.corum.backend.service.message;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.member.Member;
import com.corum.backend.domain.member.MemberRepository;
import com.corum.backend.domain.message.Message;
import com.corum.backend.domain.message.MessageRecipient;
import com.corum.backend.domain.message.MessageRecipientRepository;
import com.corum.backend.domain.message.MessageRepository;
import com.corum.backend.dto.message.ChatMessageResponse;
import com.corum.backend.dto.message.ConversationSummary;
import com.corum.backend.dto.message.MessageResponse;
import com.corum.backend.dto.message.MessageSendRequest;
import com.corum.backend.service.mail.MailService;
import com.corum.backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageRecipientRepository messageRecipientRepository;
    private final MemberRepository memberRepository;
    private final MailService mailService;
    private final NotificationService notificationService;

    // ===== 쪽지 발송 =====
    @Transactional
    public void send(MessageSendRequest request, Long senderId) {
        String title = (request.getTitle() == null || request.getTitle().isBlank())
                ? truncate(request.getContent(), 50)
                : request.getTitle();

        Message message = Message.builder()
                .senderId(senderId)
                .title(title)
                .content(request.getContent())
                .isNotice(request.getIsNotice() != null && request.getIsNotice())
                .build();
        Message saved = messageRepository.save(message);

        List<MessageRecipient> recipients = request.getRecipientIds().stream()
                .map(recipientId -> MessageRecipient.builder()
                        .messageId(saved.getId())
                        .recipientId(recipientId)
                        .build())
                .collect(Collectors.toList());
        messageRecipientRepository.saveAll(recipients);

        String senderName = memberRepository.findById(senderId)
                .map(m -> m.getName()).orElse("알 수 없음");

        memberRepository.findAllById(request.getRecipientIds()).forEach(member -> {
            try {
                mailService.send(member.getId(), member.getEmail(), "[Corum] 새 쪽지가 도착했습니다",
                        "<p><strong>" + saved.getTitle() + "</strong></p><p>Corum에서 쪽지를 확인해주세요.</p>",
                        "MESSAGE_NOTIFY");
            } catch (Exception ignored) { }
            // 실시간 알림
            try {
                notificationService.create(
                        member.getId(),
                        "MESSAGE",
                        senderName + "님이 쪽지를 보냈습니다",
                        truncate(saved.getContent(), 50),
                        "/messages"
                );
            } catch (Exception ignored) { }
        });
    }

    // ===== 받은 쪽지함 =====
    @Transactional(readOnly = true)
    public Page<MessageResponse> getInbox(Long memberId, Pageable pageable) {
        Page<MessageRecipient> page = messageRecipientRepository.findInbox(memberId, pageable);
        return toMessageResponsePage(page, pageable);
    }

    // ===== 보낸 쪽지함 =====
    @Transactional(readOnly = true)
    public Page<MessageResponse> getSent(Long memberId, Pageable pageable) {
        Page<MessageRecipient> page = messageRecipientRepository.findSent(memberId, pageable);
        return toMessageResponsePage(page, pageable);
    }

    // ===== 쪽지 상세 (읽음 처리) =====
    @Transactional
    public MessageResponse getDetail(Long recipientId, Long memberId) {
        MessageRecipient mr = messageRecipientRepository.findById(recipientId)
                .orElseThrow(() -> BusinessException.notFound("쪽지를 찾을 수 없습니다."));

        if (!mr.getRecipientId().equals(memberId)) {
            throw BusinessException.forbidden("접근 권한이 없습니다.");
        }

        if (!mr.getIsRead()) mr.markAsRead();

        Message message = messageRepository.findById(mr.getMessageId())
                .orElseThrow(() -> BusinessException.notFound("쪽지를 찾을 수 없습니다."));

        String senderName = memberRepository.findById(message.getSenderId())
                .map(Member::getName).orElse("알 수 없음");

        return new MessageResponse(mr, message, senderName);
    }

    // ===== 받은 쪽지 삭제 =====
    @Transactional
    public void deleteFromInbox(Long recipientId, Long memberId) {
        MessageRecipient mr = messageRecipientRepository.findById(recipientId)
                .orElseThrow(() -> BusinessException.notFound("쪽지를 찾을 수 없습니다."));
        if (!mr.getRecipientId().equals(memberId)) {
            throw BusinessException.forbidden("접근 권한이 없습니다.");
        }
        mr.deleteByRecipient();
    }

    // ===== 안 읽은 쪽지 수 =====
    @Transactional(readOnly = true)
    public long getUnreadCount(Long memberId) {
        return messageRecipientRepository
                .countByRecipientIdAndIsReadFalseAndIsDeletedByRecipientFalse(memberId);
    }

    // ===== 대화 목록 =====
    @Transactional(readOnly = true)
    public List<ConversationSummary> getConversations(Long memberId) {
        // 1. 내가 받은 MR 전체
        List<MessageRecipient> inboxMrs =
                messageRecipientRepository.findAllByRecipientIdAndIsDeletedByRecipientFalse(memberId);
        List<Long> inboxMsgIds = inboxMrs.stream().map(MessageRecipient::getMessageId).collect(Collectors.toList());
        Map<Long, Message> inboxMsgMap = new HashMap<>();
        if (!inboxMsgIds.isEmpty()) {
            messageRepository.findAllById(inboxMsgIds).stream()
                    .filter(m -> !m.getIsNotice())
                    .forEach(m -> inboxMsgMap.put(m.getId(), m));
        }
        Map<Long, Boolean> inboxReadMap = inboxMrs.stream()
                .collect(Collectors.toMap(MessageRecipient::getMessageId, MessageRecipient::getIsRead,
                        (a, b) -> a));

        // 2. 내가 보낸 메시지 전체 (공지 제외)
        List<Message> sentMessages =
                messageRepository.findBySenderIdAndIsNoticeFalseOrderByCreatedAtDesc(memberId);
        List<Long> sentMsgIds = sentMessages.stream().map(Message::getId).collect(Collectors.toList());
        Map<Long, Message> sentMsgMap = sentMessages.stream()
                .collect(Collectors.toMap(Message::getId, m -> m));

        // 3. 보낸 메시지의 수신자(MR) 목록
        Map<Long, List<MessageRecipient>> sentMrsByMsgId = new HashMap<>();
        if (!sentMsgIds.isEmpty()) {
            messageRecipientRepository.findAllByMessageIdIn(sentMsgIds).forEach(mr -> {
                if (!mr.getIsDeletedBySender()) {
                    sentMrsByMsgId.computeIfAbsent(mr.getMessageId(), k -> new ArrayList<>()).add(mr);
                }
            });
        }

        // 4. 파트너별 ConversationData 집계
        Map<Long, ConversationData> partnerData = new LinkedHashMap<>();

        for (MessageRecipient mr : inboxMrs) {
            Message msg = inboxMsgMap.get(mr.getMessageId());
            if (msg == null) continue;
            Long partnerId = msg.getSenderId();
            if (partnerId.equals(memberId)) continue;

            ConversationData data = partnerData.computeIfAbsent(partnerId, k -> new ConversationData());
            if (data.lastAt == null || msg.getCreatedAt().isAfter(data.lastAt)) {
                data.lastMsg    = msg;
                data.lastAt     = msg.getCreatedAt();
                data.lastIsMine = false;
            }
            if (!mr.getIsRead()) data.unreadCount++;
        }

        for (Message msg : sentMessages) {
            List<MessageRecipient> mrs = sentMrsByMsgId.getOrDefault(msg.getId(), List.of());
            for (MessageRecipient mr : mrs) {
                Long partnerId = mr.getRecipientId();
                if (partnerId.equals(memberId)) continue;

                ConversationData data = partnerData.computeIfAbsent(partnerId, k -> new ConversationData());
                if (data.lastAt == null || msg.getCreatedAt().isAfter(data.lastAt)) {
                    data.lastMsg    = msg;
                    data.lastAt     = msg.getCreatedAt();
                    data.lastIsMine = true;
                }
            }
        }

        // 5. 파트너 멤버 정보 조회
        List<Long> partnerIds = new ArrayList<>(partnerData.keySet());
        Map<Long, Member> partnerMembers = memberRepository.findAllById(partnerIds).stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        // 6. 최신 순으로 정렬해 반환
        return partnerData.entrySet().stream()
                .map(entry -> {
                    Long partnerId     = entry.getKey();
                    ConversationData d = entry.getValue();
                    Member partner     = partnerMembers.get(partnerId);
                    return new ConversationSummary(
                            partnerId,
                            partner != null ? partner.getName() : "알 수 없음",
                            partner != null ? partner.getProfileImageUrl() : null,
                            truncate(d.lastMsg.getContent(), 60),
                            d.lastAt,
                            d.unreadCount,
                            d.lastIsMine
                    );
                })
                .sorted(Comparator.comparing(ConversationSummary::getLastAt).reversed())
                .collect(Collectors.toList());
    }

    // ===== 특정 파트너와의 대화 내역 =====
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getConversation(Long me, Long partnerId) {
        List<MessageRecipient> sent     = messageRecipientRepository.findSentToPartner(me, partnerId);
        List<MessageRecipient> received = messageRecipientRepository.findReceivedFromPartner(me, partnerId);

        List<Long> allIds = new ArrayList<>();
        sent.forEach(mr -> allIds.add(mr.getMessageId()));
        received.forEach(mr -> allIds.add(mr.getMessageId()));

        Map<Long, Message> msgMap = messageRepository.findAllById(allIds).stream()
                .collect(Collectors.toMap(Message::getId, m -> m));
        Map<Long, Boolean> readMap = received.stream()
                .collect(Collectors.toMap(MessageRecipient::getMessageId, MessageRecipient::getIsRead,
                        (a, b) -> a));

        List<ChatMessageResponse> result = new ArrayList<>();
        sent.forEach(mr -> {
            Message m = msgMap.get(mr.getMessageId());
            if (m != null) result.add(new ChatMessageResponse(m, true, mr.getIsRead()));
        });
        received.forEach(mr -> {
            Message m = msgMap.get(mr.getMessageId());
            if (m != null) result.add(new ChatMessageResponse(m, false, mr.getIsRead()));
        });

        result.sort(Comparator.comparing(ChatMessageResponse::getCreatedAt));
        return result;
    }

    // ===== 파트너에게서 받은 쪽지 일괄 읽음 처리 =====
    @Transactional
    public void markConversationRead(Long me, Long partnerId) {
        messageRecipientRepository.markAllReadFromPartner(me, partnerId);
    }

    // ===== 대화 전체 삭제 (양방향) =====
    @Transactional
    public void deleteConversation(Long me, Long partnerId) {
        messageRecipientRepository.deleteReceivedFromPartner(me, partnerId);
        messageRecipientRepository.deleteSentToPartner(me, partnerId);
    }

    // ===== 내부 메서드 =====
    private Page<MessageResponse> toMessageResponsePage(Page<MessageRecipient> page, Pageable pageable) {
        List<Long> messageIds = page.getContent().stream()
                .map(MessageRecipient::getMessageId).collect(Collectors.toList());

        Map<Long, Message> messageMap = messageRepository.findAllById(messageIds).stream()
                .collect(Collectors.toMap(Message::getId, m -> m));

        List<Long> senderIds = messageMap.values().stream()
                .map(Message::getSenderId).distinct().collect(Collectors.toList());

        Map<Long, String> senderNameMap = memberRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(Member::getId, Member::getName));

        List<MessageResponse> responses = page.getContent().stream()
                .map(mr -> {
                    Message m = messageMap.get(mr.getMessageId());
                    String name = senderNameMap.getOrDefault(m.getSenderId(), "알 수 없음");
                    return new MessageResponse(mr, m, name);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, page.getTotalElements());
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private static class ConversationData {
        Message lastMsg;
        java.time.LocalDateTime lastAt;
        boolean lastIsMine;
        long unreadCount;
    }
}
