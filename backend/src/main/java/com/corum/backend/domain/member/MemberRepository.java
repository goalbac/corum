package com.corum.backend.domain.member;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByUsername(String username);
    Optional<Member> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    Page<Member> findByNameContainingOrEmailContaining(
            String name, String email, Pageable pageable);

    Page<Member> findByNameContainingOrUsernameContainingOrEmailContaining(
            String name, String username, String email, Pageable pageable);

    // 쪽지 수신자 검색 (이름 또는 아이디 포함, 활성 회원만)
    @org.springframework.data.jpa.repository.Query("""
        SELECT m FROM Member m
        WHERE m.isActive = true
          AND (LOWER(m.name) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(m.username) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY m.name ASC
        """)
    List<Member> searchActive(@org.springframework.data.repository.query.Param("q") String q,
                              Pageable pageable);

    long countByJoinedAtBetween(LocalDateTime from, LocalDateTime to);
}
