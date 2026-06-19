package com.corum.backend.dto.inquiry;

import com.corum.backend.domain.inquiry.InquiryMemo;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class InquiryMemoResponse {

    private final Long id;
    private final Long createdBy;
    private final String createdByName;
    private final String memo;
    private final LocalDateTime createdAt;

    public InquiryMemoResponse(InquiryMemo memo, String createdByName) {
        this.id            = memo.getId();
        this.createdBy     = memo.getCreatedBy();
        this.createdByName = createdByName;
        this.memo          = memo.getMemo();
        this.createdAt     = memo.getCreatedAt();
    }
}
