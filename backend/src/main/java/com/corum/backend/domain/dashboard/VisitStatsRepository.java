package com.corum.backend.domain.dashboard;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface VisitStatsRepository extends JpaRepository<VisitStats, Long> {
    Optional<VisitStats> findByStatDate(LocalDate statDate);
}
