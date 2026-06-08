package com.corum.backend.domain.log;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {
    Page<SearchLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
