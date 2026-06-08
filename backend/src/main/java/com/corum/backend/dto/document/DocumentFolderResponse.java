package com.corum.backend.dto.document;

import com.corum.backend.domain.document.DocumentFolder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DocumentFolderResponse {

    private final Long id;
    private final Long boardId;
    private final Long parentId;
    private final String name;
    private final Integer sortOrder;
    private final LocalDateTime createdAt;

    public DocumentFolderResponse(DocumentFolder folder) {
        this.id = folder.getId();
        this.boardId = folder.getBoardId();
        this.parentId = folder.getParentId();
        this.name = folder.getName();
        this.sortOrder = folder.getSortOrder();
        this.createdAt = folder.getCreatedAt();
    }
}
