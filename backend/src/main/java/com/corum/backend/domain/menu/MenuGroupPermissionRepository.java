package com.corum.backend.domain.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuGroupPermissionRepository extends JpaRepository<MenuGroupPermission, Long> {

    List<MenuGroupPermission> findByMenuId(Long menuId);

    List<MenuGroupPermission> findByMenuIdIn(List<Long> menuIds);

    boolean existsByMenuIdAndGroupIdIn(Long menuId, List<Long> groupIds);

    void deleteByMenuId(Long menuId);
}
