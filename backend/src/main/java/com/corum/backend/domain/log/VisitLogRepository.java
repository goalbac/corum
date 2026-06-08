package com.corum.backend.domain.log;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface VisitLogRepository extends JpaRepository<VisitLog, Long> {
    Page<VisitLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    boolean existsByVisitDateAndIpAddress(LocalDate visitDate, String ipAddress);
}
