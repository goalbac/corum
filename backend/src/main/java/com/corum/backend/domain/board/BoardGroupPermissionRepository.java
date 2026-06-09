package com.corum.backend.domain.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface BoardGroupPermissionRepository extends JpaRepository<BoardGroupPermission, Long> {

    List<BoardGroupPermission> findByBoardId(Long boardId);

    @Transactional
    void deleteByBoardId(Long boardId);

    @Query("""
        SELECT p FROM BoardGroupPermission p
        WHERE p.boardId = :boardId AND p.groupId IN :groupIds
        """)
    List<BoardGroupPermission> findByBoardIdAndGroupIds(Long boardId, List<Long> groupIds);

    /** 게시판에 대해 해당 그룹들 중 can_manage = true 인 행이 존재하는지 */
    @Query("""
        SELECT COUNT(p) > 0 FROM BoardGroupPermission p
        WHERE p.boardId = :boardId AND p.groupId IN :groupIds AND p.canManage = true
        """)
    boolean existsManagePermission(Long boardId, List<Long> groupIds);
}
