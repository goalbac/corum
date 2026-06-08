package com.corum.backend.dto.document;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentVersionRequest {

    @NotNull
    private Long fileId;

    private String changeNote;
}
