package com.corum.backend.domain.log;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface VisitLogRepository extends JpaRepository<VisitLog, Long> {
    Page<VisitLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    boolean existsByVisitDateAndIpAddress(LocalDate visitDate, String ipAddress);

    // 기간 합계에서는 날짜별 unique_visits를 그대로 더하면 같은 방문자가 여러 날 방문한 만큼
    // 중복 집계되므로, 기간 전체 기준 distinct IP 수를 별도로 계산한다
    @Query("SELECT COUNT(DISTINCT v.ipAddress) FROM VisitLog v WHERE v.visitDate BETWEEN :from AND :to")
    long countDistinctIpByVisitDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
