package com.corum.backend.domain.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MemberFavoriteMenuRepository extends JpaRepository<MemberFavoriteMenu, Long> {

    List<MemberFavoriteMenu> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    boolean existsByMemberIdAndMenuId(Long memberId, Long menuId);

    void deleteByMemberIdAndMenuId(Long memberId, Long menuId);

    @Query("SELECT f.menuId FROM MemberFavoriteMenu f WHERE f.memberId = :memberId ORDER BY f.createdAt DESC")
    List<Long> findMenuIdsByMemberId(Long memberId);
}
