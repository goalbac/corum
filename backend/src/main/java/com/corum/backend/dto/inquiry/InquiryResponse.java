package com.corum.backend.dto.inquiry;

import com.corum.backend.domain.inquiry.Inquiry;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class InquiryResponse {

    private final Long id;
    private final Long memberId;
    private final String writerName;
    private final String title;
    private final String content;
    private final String contactPhone;
    private final String contactEmail;
    private final String clientIp;
    private final String inquiryType;
    private final String status;
    private final LocalDateTime createdAt;
    private final List<InquiryMemoResponse> memos;

    // 답변
    private final String replyContent;
    private final LocalDateTime repliedAt;
    private final String repliedByName;

    public InquiryResponse(Inquiry inquiry, List<InquiryMemoResponse> memos) {
        this(inquiry, memos, null);
    }

    public InquiryResponse(Inquiry inquiry, List<InquiryMemoResponse> memos, String repliedByName) {
        this.id             = inquiry.getId();
        this.memberId       = inquiry.getMemberId();
        this.writerName     = inquiry.getWriterName();
        this.title          = inquiry.getTitle();
        this.content        = inquiry.getContent();
        this.contactPhone   = inquiry.getContactPhone();
        this.contactEmail   = inquiry.getContactEmail();
        this.clientIp       = inquiry.getClientIp();
        this.inquiryType    = inquiry.getInquiryType();
        this.status         = inquiry.getStatus();
        this.createdAt      = inquiry.getCreatedAt();
        this.memos          = memos;
        this.replyContent   = inquiry.getReplyContent();
        this.repliedAt      = inquiry.getRepliedAt();
        this.repliedByName  = repliedByName;
    }
}
