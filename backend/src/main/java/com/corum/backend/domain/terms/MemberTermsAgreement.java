package com.corum.backend.domain.terms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_terms_agreements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class MemberTermsAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "terms_id", nullable = false)
    private Long termsId;

    @Builder.Default
    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt = LocalDateTime.now();

    @Column(name = "agreed_ip", length = 50)
    private String agreedIp;
}
