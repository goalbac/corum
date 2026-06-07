package com.corum.backend.dto.inquiry;

import com.corum.backend.domain.inquiry.Inquiry;
import com.corum.backend.domain.inquiry.InquiryMemo;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class InquiryResponse {

    private final Long id;
    private final Long memberId;
    private final String title;
    private final String content;
    private final String contactPhone;
    private final String contactEmail;
    private final String clientIp;
    private final String status;
    private final LocalDateTime createdAt;
    private final List<InquiryMemo> memos;

    public InquiryResponse(Inquiry inquiry, List<InquiryMemo> memos) {
        this.id           = inquiry.getId();
        this.memberId     = inquiry.getMemberId();
        this.title        = inquiry.getTitle();
        this.content      = inquiry.getContent();
        this.contactPhone = inquiry.getContactPhone();
        this.contactEmail = inquiry.getContactEmail();
        this.clientIp     = inquiry.getClientIp();
        this.status       = inquiry.getStatus();
        this.createdAt    = inquiry.getCreatedAt();
        this.memos        = memos;
    }
}
