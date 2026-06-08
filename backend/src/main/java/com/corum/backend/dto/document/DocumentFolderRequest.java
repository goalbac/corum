package com.corum.backend.dto.document;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentFolderRequest {

    private Long parentId;

    @NotBlank
    private String name;

    private Integer sortOrder = 0;
}
