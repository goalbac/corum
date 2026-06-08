package com.corum.backend.dto.document;

import com.corum.backend.domain.document.DocumentVersion;
import com.corum.backend.dto.file.FileResponse;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DocumentVersionResponse {

    private final Long id;
    private final Long postId;
    private final Integer versionNumber;
    private final Long fileId;
    private final String changeNote;
    private final Long createdBy;
    private final LocalDateTime createdAt;
    private final FileResponse file;

    public DocumentVersionResponse(DocumentVersion version, FileResponse file) {
        this.id = version.getId();
        this.postId = version.getPostId();
        this.versionNumber = version.getVersionNumber();
        this.fileId = version.getFileId();
        this.changeNote = version.getChangeNote();
        this.createdBy = version.getCreatedBy();
        this.createdAt = version.getCreatedAt();
        this.file = file;
    }
}
