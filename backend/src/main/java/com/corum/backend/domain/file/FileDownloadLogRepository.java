package com.corum.backend.domain.file;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FileDownloadLogRepository extends JpaRepository<FileDownloadLog, Long> {
}
