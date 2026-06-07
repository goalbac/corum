package com.corum.backend.dto.auth;

import com.corum.backend.domain.member.Member;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class MemberResponse {

    private final Long id;
    private final String username;
    private final String email;
    private final String name;
    private final String gender;
    private final String phone;
    private final String address;
    private final LocalDate birthDate;
    private final String homePhone;
    private final String occupation;
    private final String workPhone;
    private final Boolean newsletterYn;
    private final String profileImageUrl;
    private final Boolean isActive;
    private final Boolean isLocked;
    private final LocalDateTime joinedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    @com.fasterxml.jackson.annotation.JsonProperty("isAdmin")
    private final boolean isAdmin;

    public MemberResponse(Member member, boolean isAdmin) {
        this.id = member.getId();
        this.username = member.getUsername();
        this.email = member.getEmail();
        this.name = member.getName();
        this.gender = member.getGender();
        this.phone = member.getPhone();
        this.address = member.getAddress();
        this.birthDate = member.getBirthDate();
        this.homePhone = member.getHomePhone();
        this.occupation = member.getOccupation();
        this.workPhone = member.getWorkPhone();
        this.newsletterYn = member.getNewsletterYn();
        this.profileImageUrl = member.getProfileImageUrl();
        this.isActive = member.getIsActive();
        this.isLocked = member.getIsLocked();
        this.joinedAt = member.getJoinedAt();
        this.createdAt = member.getCreatedAt();
        this.updatedAt = member.getUpdatedAt();
        this.isAdmin = isAdmin;
    }
}