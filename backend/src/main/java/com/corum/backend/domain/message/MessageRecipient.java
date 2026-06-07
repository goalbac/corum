package com.corum.backend.domain.message;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_recipients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class MessageRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "is_deleted_by_sender", nullable = false)
    @Builder.Default
    private Boolean isDeletedBySender = false;

    @Column(name = "is_deleted_by_recipient", nullable = false)
    @Builder.Default
    private Boolean isDeletedByRecipient = false;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    public void deleteByRecipient() {
        this.isDeletedByRecipient = true;
    }

    public void deleteBySender() {
        this.isDeletedBySender = true;
    }
}
