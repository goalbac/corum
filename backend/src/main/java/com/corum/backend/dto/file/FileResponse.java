package com.corum.backend.dto.file;

import com.corum.backend.domain.file.UploadFile;
import lombok.Getter;

@Getter
public class FileResponse {

    private final Long id;
    private final String originalName;
    private final String mimeType;
    private final Long fileSize;
    private final Integer downloadCount;
    private final String downloadUrl;
    private final String thumbnailUrl;

    public FileResponse(UploadFile file) {
        this.id = file.getId();
        this.originalName = file.getOriginalName();
        this.mimeType = file.getMimeType();
        this.fileSize = file.getFileSize();
        this.downloadCount = file.getDownloadCount();
        this.downloadUrl = "/api/files/" + file.getId() + "/download";
        this.thumbnailUrl = file.getThumbnailPath() != null
                ? "/api/files/" + file.getId() + "/thumbnail"
                : "/api/files/" + file.getId() + "/view";
    }
}
