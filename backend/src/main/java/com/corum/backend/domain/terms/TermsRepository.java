package com.corum.backend.domain.terms;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TermsRepository extends JpaRepository<Terms, Long> {
    List<Terms> findAllByOrderByTypeAscVersionDesc();
    List<Terms> findByIsActiveTrueOrderByTypeAsc();
    Optional<Terms> findTopByTypeOrderByVersionDesc(String type);
    List<Terms> findByTypeAndIsActiveTrue(String type);
    List<Terms> findByTypeAndIsActiveTrueOrderByVersionDesc(String type);
}
