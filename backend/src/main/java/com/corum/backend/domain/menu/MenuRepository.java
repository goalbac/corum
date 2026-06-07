package com.corum.backend.domain.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findAllByOrderBySortOrderAsc();

    List<Menu> findByParentIdIsNullAndIsActiveTrueOrderBySortOrderAsc();

    List<Menu> findByParentIdAndIsActiveTrueOrderBySortOrderAsc(Long parentId);

    List<Menu> findByIsActiveTrueOrderBySortOrderAsc();
}
