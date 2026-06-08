package com.corum.backend.dto.terms;

import com.corum.backend.domain.terms.Terms;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TermsResponse {
    private final Long id;
    private final String type;
    private final Integer version;
    private final String content;
    private final Boolean isActive;
    private final Boolean requireReagree;
    private final LocalDateTime createdAt;

    public TermsResponse(Terms terms) {
        this.id = terms.getId();
        this.type = terms.getType();
        this.version = terms.getVersion();
        this.content = terms.getContent();
        this.isActive = terms.getIsActive();
        this.requireReagree = terms.getRequireReagree();
        this.createdAt = terms.getCreatedAt();
    }
}
