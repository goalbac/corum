package com.corum.backend.dto.member;

import com.corum.backend.domain.member.Member;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class MemberListResponse {

    private final Long id;
    private final String username;
    private final String name;
    private final String email;
    private final String phone;
    private final Boolean isActive;
    private final Boolean isLocked;
    private final LocalDateTime joinedAt;
    private final LocalDateTime withdrawnAt;
    private final List<GroupSimple> groups;

    public MemberListResponse(Member member, List<GroupSimple> groups) {
        this.id          = member.getId();
        this.username    = member.getUsername();
        this.name        = member.getName();
        this.email       = member.getEmail();
        this.phone       = member.getPhone();
        this.isActive    = member.getIsActive();
        this.isLocked    = member.getIsLocked();
        this.joinedAt    = member.getJoinedAt();
        this.withdrawnAt = member.getWithdrawnAt();
        this.groups      = groups;
    }

    @Getter
    public static class GroupSimple {
        private final Long id;
        private final String name;
        private final String type;

        public GroupSimple(Long id, String name, String type) {
            this.id   = id;
            this.name = name;
            this.type = type;
        }
    }
}
