package com.corum.backend.domain.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminMenuRepository extends JpaRepository<AdminMenu, Long> {
    List<AdminMenu> findAllByOrderBySortOrderAsc();

    Optional<AdminMenu> findByUrl(String url);
}
