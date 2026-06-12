package com.corum.backend.domain.board;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardCategoryRepository extends JpaRepository<BoardCategory, Long> {

    List<BoardCategory> findByBoardIdOrderBySortOrderAsc(Long boardId);

    void deleteByBoardId(Long boardId);
}
