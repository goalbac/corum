package com.corum.backend.domain.message;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRecipientRepository extends JpaRepository<MessageRecipient, Long> {

    // 받은 쪽지함 (페이징)
    @Query("""
        SELECT mr FROM MessageRecipient mr
        WHERE mr.recipientId = :memberId AND mr.isDeletedByRecipient = false
        ORDER BY mr.createdAt DESC
        """)
    Page<MessageRecipient> findInbox(Long memberId, Pageable pageable);

    // 보낸 쪽지함 (페이징)
    @Query("""
        SELECT mr FROM MessageRecipient mr
        JOIN Message m ON m.id = mr.messageId
        WHERE m.senderId = :memberId AND mr.isDeletedBySender = false
        ORDER BY mr.createdAt DESC
        """)
    Page<MessageRecipient> findSent(Long memberId, Pageable pageable);

    // 안 읽은 쪽지 수
    long countByRecipientIdAndIsReadFalseAndIsDeletedByRecipientFalse(Long recipientId);

    // 첨부파일 접근 권한 확인용: 이 회원이 해당 쪽지의 수신자인지
    boolean existsByMessageIdAndRecipientId(Long messageId, Long recipientId);

    // 대화 목록용: 내가 받은 모든 MR (삭제 안 된 것)
    List<MessageRecipient> findAllByRecipientIdAndIsDeletedByRecipientFalse(Long recipientId);

    // 특정 message ID 목록에 속한 모든 MR
    List<MessageRecipient> findAllByMessageIdIn(List<Long> messageIds);

    // me → partner 방향 MR (내가 보낸 것)
    @Query("""
        SELECT mr FROM MessageRecipient mr
        JOIN Message m ON m.id = mr.messageId
        WHERE m.senderId = :me AND mr.recipientId = :partner AND mr.isDeletedBySender = false
        """)
    List<MessageRecipient> findSentToPartner(@Param("me") Long me, @Param("partner") Long partner);

    // partner → me 방향 MR (내가 받은 것)
    @Query("""
        SELECT mr FROM MessageRecipient mr
        JOIN Message m ON m.id = mr.messageId
        WHERE mr.recipientId = :me AND m.senderId = :partner AND mr.isDeletedByRecipient = false
        """)
    List<MessageRecipient> findReceivedFromPartner(@Param("me") Long me, @Param("partner") Long partner);

    // partner에게서 받은 읽지 않은 쪽지 전부 읽음 처리
    @Modifying
    @Query("""
        UPDATE MessageRecipient mr
        SET mr.isRead = true
        WHERE mr.recipientId = :me AND mr.isRead = false
          AND mr.messageId IN (SELECT m.id FROM Message m WHERE m.senderId = :partner)
        """)
    int markAllReadFromPartner(@Param("me") Long me, @Param("partner") Long partner);

    // 대화 삭제 - 내가 받은 쪽지 (partner → me) 전부 삭제 처리
    @Modifying
    @Query("""
        UPDATE MessageRecipient mr
        SET mr.isDeletedByRecipient = true
        WHERE mr.recipientId = :me
          AND mr.messageId IN (SELECT m.id FROM Message m WHERE m.senderId = :partner)
        """)
    int deleteReceivedFromPartner(@Param("me") Long me, @Param("partner") Long partner);

    // 대화 삭제 - 내가 보낸 쪽지 (me → partner) 전부 삭제 처리
    @Modifying
    @Query("""
        UPDATE MessageRecipient mr
        SET mr.isDeletedBySender = true
        WHERE mr.recipientId = :partner
          AND mr.messageId IN (SELECT m.id FROM Message m WHERE m.senderId = :me)
        """)
    int deleteSentToPartner(@Param("me") Long me, @Param("partner") Long partner);
}
