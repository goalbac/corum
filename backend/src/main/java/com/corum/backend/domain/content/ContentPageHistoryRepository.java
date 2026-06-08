package com.corum.backend.domain.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentPageHistoryRepository extends JpaRepository<ContentPageHistory, Long> {
    List<ContentPageHistory> findByContentPageIdOrderByCreatedAtDesc(Long contentPageId);
}
