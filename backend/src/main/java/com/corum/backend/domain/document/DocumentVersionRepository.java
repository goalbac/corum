package com.corum.backend.domain.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    List<DocumentVersion> findByPostIdOrderByVersionNumberDesc(Long postId);

    @Query("SELECT COALESCE(MAX(v.versionNumber), 0) FROM DocumentVersion v WHERE v.postId = :postId")
    Integer findMaxVersionNumber(Long postId);
}
