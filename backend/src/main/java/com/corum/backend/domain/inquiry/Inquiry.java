package com.corum.backend.domain.inquiry;

import com.corum.backend.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inquiries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Inquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // null: 비로그인 접수
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "writer_name", length = 100)
    private String writerName;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "client_ip", length = 50)
    private String clientIp;

    // INQUIRY / BUG_REPORT / FEATURE_REQUEST
    @Column(name = "inquiry_type", nullable = false, length = 30)
    @Builder.Default
    private String inquiryType = "INQUIRY";

    // RECEIVED, CHECKING, DONE
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "RECEIVED";

    @Column(name = "reply_content", columnDefinition = "TEXT")
    private String replyContent;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    @Column(name = "replied_by")
    private Long repliedBy;

    public void updateStatus(String status) {
        this.status = status;
    }

    public void writeReply(String content, Long adminId) {
        this.replyContent = content;
        this.repliedAt    = LocalDateTime.now();
        this.repliedBy    = adminId;
        if ("RECEIVED".equals(this.status)) this.status = "CHECKING";
    }
}
