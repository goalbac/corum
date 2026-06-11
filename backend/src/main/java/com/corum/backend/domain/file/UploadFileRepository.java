package com.corum.backend.domain.file;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UploadFileRepository extends JpaRepository<UploadFile, Long> {

    List<UploadFile> findByTargetTypeAndTargetId(String targetType, Long targetId);

    void deleteByTargetTypeAndTargetId(String targetType, Long targetId);

    int countByTargetTypeAndTargetId(String targetType, Long targetId);

    @Query("SELECT f FROM UploadFile f WHERE f.targetType = 'POST' AND f.targetId IN :postIds AND f.mimeType LIKE 'image%' ORDER BY f.targetId ASC, f.id ASC")
    List<UploadFile> findImageFilesByPostIds(@Param("postIds") List<Long> postIds);
}
