package com.corum.backend.domain.board;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardWriterIdentityRepository extends JpaRepository<BoardWriterIdentity, Long> {

    List<BoardWriterIdentity> findByBoardIdOrderBySortOrderAsc(Long boardId);

    void deleteByBoardId(Long boardId);
}
