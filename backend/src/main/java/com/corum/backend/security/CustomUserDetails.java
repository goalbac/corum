package com.corum.backend.security;

import com.corum.backend.domain.member.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Member member;
    private final boolean admin;
    private final boolean superAdmin;

    public CustomUserDetails(Member member, boolean admin, boolean superAdmin) {
        this.member = member;
        this.admin = admin;
        this.superAdmin = superAdmin;
    }

    public Long getMemberId() {
        return member.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (admin) authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        if (superAdmin) authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        return authorities;
    }

    @Override public String getPassword()  { return member.getPasswordHash(); }
    @Override public String getUsername()  { return member.getUsername(); }
    @Override public boolean isAccountNonExpired()   { return true; }
    @Override public boolean isAccountNonLocked()    { return !member.getIsLocked(); }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()             { return member.getIsActive(); }
}
