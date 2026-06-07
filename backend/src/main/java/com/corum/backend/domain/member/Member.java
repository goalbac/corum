package com.corum.backend.domain.member;

import com.corum.backend.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "home_phone", length = 30)
    private String homePhone;

    @Column(name = "occupation", length = 200)
    private String occupation;

    @Column(name = "work_phone", length = 30)
    private String workPhone;

    @Column(name = "newsletter_yn", nullable = false)
    @Builder.Default
    private Boolean newsletterYn = false;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = false;

    @Column(name = "login_fail_count", nullable = false)
    @Builder.Default
    private Integer loginFailCount = 0;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "joined_at", nullable = false)
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Column(name = "withdrawn_ip", length = 50)
    private String withdrawnIp;

    // ===== 비즈니스 메서드 =====

    public void activate() {
        this.isActive = true;
    }

    public void lock(LocalDateTime lockedAt) {
        this.isLocked = true;
        this.lockedAt = lockedAt;
    }

    public void unlock() {
        this.isLocked = false;
        this.lockedAt = null;
        this.loginFailCount = 0;
    }

    public void increaseLoginFailCount() {
        this.loginFailCount++;
    }

    public void resetLoginFailCount() {
        this.loginFailCount = 0;
    }

    public void updatePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void updateProfile(String name, String phone, String address,
                               LocalDate birthDate, String homePhone,
                               String occupation, String workPhone,
                               Boolean newsletterYn, String profileImageUrl) {
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.birthDate = birthDate;
        this.homePhone = homePhone;
        this.occupation = occupation;
        this.workPhone = workPhone;
        this.newsletterYn = newsletterYn;
        this.profileImageUrl = profileImageUrl;
    }

    public void withdraw(String ip) {
        this.withdrawnAt = LocalDateTime.now();
        this.withdrawnIp = ip;
        this.isActive = false;
    }
}
