package com.corum.backend.domain.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {

    @Transactional
    @Modifying
    @Query("DELETE FROM InvalidatedToken t WHERE t.expiresAt < :cutoff")
    int deleteByExpiresAtBefore(LocalDateTime cutoff);
}
