package com.corum.backend.domain.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BoardGroupPermissionRepository extends JpaRepository<BoardGroupPermission, Long> {

    List<BoardGroupPermission> findByBoardId(Long boardId);

    void deleteByBoardId(Long boardId);

    @Query("""
        SELECT p FROM BoardGroupPermission p
        WHERE p.boardId = :boardId AND p.groupId IN :groupIds
        """)
    List<BoardGroupPermission> findByBoardIdAndGroupIds(Long boardId, List<Long> groupIds);
}
