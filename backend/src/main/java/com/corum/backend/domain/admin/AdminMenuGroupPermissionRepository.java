package com.corum.backend.domain.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AdminMenuGroupPermissionRepository extends JpaRepository<AdminMenuGroupPermission, Long> {

    List<AdminMenuGroupPermission> findByAdminMenuId(Long adminMenuId);

    List<AdminMenuGroupPermission> findByGroupId(Long groupId);

    List<AdminMenuGroupPermission> findByGroupIdIn(List<Long> groupIds);

    Optional<AdminMenuGroupPermission> findByAdminMenuIdAndGroupId(Long adminMenuId, Long groupId);

    void deleteByAdminMenuId(Long adminMenuId);

    /** 현재 사용자(그룹 목록)가 조회 가능한 admin_menu_id 목록 */
    @Query("""
        SELECT DISTINCT p.adminMenuId FROM AdminMenuGroupPermission p
        WHERE p.groupId IN :groupIds AND p.canView = true
        """)
    List<Long> findViewableMenuIds(List<Long> groupIds);
}
