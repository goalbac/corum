package com.corum.backend.domain.dashboard;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "visit_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "total_visits", nullable = false)
    private Integer totalVisits;

    @Column(name = "unique_visits", nullable = false)
    private Integer uniqueVisits;

    @Column(name = "login_visits", nullable = false)
    private Integer loginVisits;
}
