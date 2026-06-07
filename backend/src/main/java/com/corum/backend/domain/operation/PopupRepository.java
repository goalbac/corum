package com.corum.backend.domain.operation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PopupRepository extends JpaRepository<Popup, Long> {
    List<Popup> findAllByOrderByPriorityAscCreatedAtDesc();
}
