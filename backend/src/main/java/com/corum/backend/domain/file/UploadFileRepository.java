package com.corum.backend.domain.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UploadFileRepository extends JpaRepository<UploadFile, Long> {

    List<UploadFile> findByTargetTypeAndTargetId(String targetType, Long targetId);

    void deleteByTargetTypeAndTargetId(String targetType, Long targetId);

    int countByTargetTypeAndTargetId(String targetType, Long targetId);
}
