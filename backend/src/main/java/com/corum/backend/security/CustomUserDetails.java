package com.corum.backend.security;

import com.corum.backend.domain.member.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Member member;

    public CustomUserDetails(Member member) {
        this.member = member;
    }

    public Long getMemberId() {
        return member.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 그룹 기반 권한은 DB에서 동적으로 처리 — 여기선 빈 목록
        return List.of();
    }

    @Override public String getPassword()  { return member.getPasswordHash(); }
    @Override public String getUsername()  { return member.getUsername(); }
    @Override public boolean isAccountNonExpired()   { return true; }
    @Override public boolean isAccountNonLocked()    { return !member.getIsLocked(); }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()             { return member.getIsActive(); }
}
