package com.corum.backend.domain.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findAllByOrderBySortOrderAsc();

    List<Group> findByParentIdOrderBySortOrderAsc(Long parentId);

    List<Group> findByParentIdIsNullOrderBySortOrderAsc();

    boolean existsByName(String name);

    List<Group> findByType(String type);

    // 특정 타입의 최상위 그룹
    @Query("SELECT g FROM Group g WHERE g.parentId IS NULL AND g.type = :type ORDER BY g.sortOrder ASC")
    List<Group> findRootGroupsByType(String type);
}
