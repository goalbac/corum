package com.corum.backend.domain.log;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmtpSendLogRepository extends JpaRepository<SmtpSendLog, Long> {
    Page<SmtpSendLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
