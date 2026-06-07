package com.corum.backend.dto.auth;

import com.corum.backend.domain.member.Member;
import lombok.Getter;

@Getter
public class LoginResponse {

    private final String accessToken;
    private final MemberInfo member;

    public LoginResponse(String accessToken, Member member) {
        this.accessToken = accessToken;
        this.member = new MemberInfo(member);
    }

    @Getter
    public static class MemberInfo {
        private final Long id;
        private final String username;
        private final String name;
        private final String email;
        private final String profileImageUrl;

        public MemberInfo(Member member) {
            this.id = member.getId();
            this.username = member.getUsername();
            this.name = member.getName();
            this.email = member.getEmail();
            this.profileImageUrl = member.getProfileImageUrl();
        }
    }
}
