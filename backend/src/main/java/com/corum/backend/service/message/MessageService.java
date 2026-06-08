package com.corum.backend.service.message;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.member.Member;
import com.corum.backend.domain.member.MemberRepository;
import com.corum.backend.domain.message.Message;
import com.corum.backend.domain.message.MessageRecipient;
import com.corum.backend.domain.message.MessageRecipientRepository;
import com.corum.backend.domain.message.MessageRepository;
import com.corum.backend.dto.message.MessageResponse;
import com.corum.backend.dto.message.MessageSendRequest;
import com.corum.backend.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageRecipientRepository messageRecipientRepository;
    private final MemberRepository memberRepository;
    private final MailService mailService;

    // ===== 쪽지 발송 =====
    @Transactional
    public void send(MessageSendRequest request, Long senderId) {
        Message message = Message.builder()
                .senderId(senderId)
                .title(request.getTitle())
                .content(request.getContent())
                .isNotice(request.getIsNotice())
                .build();
        Message saved = messageRepository.save(message);

        List<MessageRecipient> recipients = request.getRecipientIds().stream()
                .map(recipientId -> MessageRecipient.builder()
                        .messageId(saved.getId())
                        .recipientId(recipientId)
                        .build())
                .collect(Collectors.toList());
        messageRecipientRepository.saveAll(recipients);

        memberRepository.findAllById(request.getRecipientIds()).forEach(member -> {
            try {
                mailService.send(member.getId(), member.getEmail(), "[Corum] 새 쪽지가 도착했습니다",
                        "<p><strong>" + saved.getTitle() + "</strong></p><p>Corum에서 쪽지를 확인해주세요.</p>",
                        "MESSAGE_NOTIFY");
            } catch (Exception ignored) {
                // 메일 실패가 쪽지 발송을 막지 않도록 한다.
            }
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
}
