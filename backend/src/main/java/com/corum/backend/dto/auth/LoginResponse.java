package com.corum.backend.dto.auth;

import com.corum.backend.domain.member.Member;
import com.corum.backend.dto.group.GroupResponse;
import com.corum.backend.dto.terms.TermsResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class LoginResponse {

    private final String accessToken;
    private final MemberInfo member;

    public LoginResponse(String accessToken, Member member, List<GroupResponse> groups) {
        this(accessToken, member, groups, List.of());
    }

    public LoginResponse(String accessToken, Member member, List<GroupResponse> groups, List<TermsResponse> requiredTerms) {
        this.accessToken = accessToken;
        this.member = new MemberInfo(member, groups, requiredTerms);
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
        private final boolean requiresTermsAgreement;
        private final List<TermsResponse> requiredTerms;
        private final boolean mustChangePassword;

        public MemberInfo(Member member, List<GroupResponse> groups, List<TermsResponse> requiredTerms) {
            this.id = member.getId();
            this.username = member.getUsername();
            this.name = member.getName();
            this.email = member.getEmail();
            this.profileImageUrl = member.getProfileImageUrl();
            this.groups = groups;
            this.isAdmin = groups.stream()
                    .anyMatch(g -> "ADMIN".equals(g.getType()));
            this.requiredTerms = requiredTerms;
            this.requiresTermsAgreement = requiredTerms != null && !requiredTerms.isEmpty();
            this.mustChangePassword = Boolean.TRUE.equals(member.getMustChangePassword());
        }
    }
}
