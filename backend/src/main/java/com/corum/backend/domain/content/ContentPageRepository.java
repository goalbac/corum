package com.corum.backend.domain.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContentPageRepository extends JpaRepository<ContentPage, Long> {
    Optional<ContentPage> findByMenuId(Long menuId);
}
