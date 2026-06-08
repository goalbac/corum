package com.corum.backend.domain.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentFolderRepository extends JpaRepository<DocumentFolder, Long> {

    List<DocumentFolder> findByBoardIdOrderBySortOrderAscIdAsc(Long boardId);

    boolean existsByIdAndBoardId(Long id, Long boardId);
}
