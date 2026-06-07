package com.corum.backend.domain.inquiry;

import com.corum.backend.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

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

    // RECEIVED, IN_PROGRESS, COMPLETED
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "RECEIVED";

    public void updateStatus(String status) {
        this.status = status;
    }
}
