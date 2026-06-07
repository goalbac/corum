package com.corum.backend.dto.auth;

import com.corum.backend.domain.member.Member;
import com.corum.backend.dto.group.GroupResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class LoginResponse {

    private final String accessToken;
    private final MemberInfo member;

    public LoginResponse(String accessToken, Member member, List<GroupResponse> groups) {
        this.accessToken = accessToken;
        this.member = new MemberInfo(member, groups);
    }

    @Getter
    public static class MemberInfo {
        private final Long id;
        private final String username;
        private final String name;
        private final String email;
        private final String profileImageUrl;
        private final boolean isAdmin;
        private final List<GroupResponse> groups;

        public MemberInfo(Member member, List<GroupResponse> groups) {
            this.id = member.getId();
            this.username = member.getUsername();
            this.name = member.getName();
            this.email = member.getEmail();
            this.profileImageUrl = member.getProfileImageUrl();
            this.groups = groups;
            this.isAdmin = groups.stream()
                    .anyMatch(g -> "ADMIN".equals(g.getType()));
        }
    }
}
