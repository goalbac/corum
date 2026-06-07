package com.corum.backend.domain.message;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MessageRecipientRepository extends JpaRepository<MessageRecipient, Long> {

    // 받은 쪽지함
    @Query("""
        SELECT mr FROM MessageRecipient mr
        WHERE mr.recipientId = :memberId AND mr.isDeletedByRecipient = false
        ORDER BY mr.createdAt DESC
        """)
    Page<MessageRecipient> findInbox(Long memberId, Pageable pageable);

    // 보낸 쪽지함
    @Query("""
        SELECT mr FROM MessageRecipient mr
        JOIN Message m ON m.id = mr.messageId
        WHERE m.senderId = :memberId AND mr.isDeletedBySender = false
        ORDER BY mr.createdAt DESC
        """)
    Page<MessageRecipient> findSent(Long memberId, Pageable pageable);

    // 안 읽은 쪽지 수
    long countByRecipientIdAndIsReadFalseAndIsDeletedByRecipientFalse(Long recipientId);
}
