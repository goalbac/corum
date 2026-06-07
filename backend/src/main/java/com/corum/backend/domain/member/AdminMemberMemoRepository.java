package com.corum.backend.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminMemberMemoRepository extends JpaRepository<AdminMemberMemo, Long> {
    Optional<AdminMemberMemo> findByMemberId(Long memberId);
}
