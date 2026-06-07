package com.corum.backend.domain.board;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {

    List<Board> findByIsActiveTrueOrderByIdAsc();

    Optional<Board> findByMenuId(Long menuId);
}
