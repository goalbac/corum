package com.corum.backend.dto.member;

import com.corum.backend.domain.group.Group;
import com.corum.backend.domain.member.Member;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class MemberPublicResponse {
    private final Long id;
    private final String name;
    private final String username;
    private final String profileImageUrl;
    private final List<GroupInfo> groups;

    public MemberPublicResponse(Member member, List<Group> groups) {
        this.id               = member.getId();
        this.name             = member.getName();
        this.username         = member.getUsername();
        this.profileImageUrl  = member.getProfileImageUrl();
        this.groups           = groups.stream()
                .map(g -> new GroupInfo(g.getId(), g.getName()))
                .collect(Collectors.toList());
    }

    @Getter
    @lombok.AllArgsConstructor
    public static class GroupInfo {
        private final Long id;
        private final String name;
    }
}
