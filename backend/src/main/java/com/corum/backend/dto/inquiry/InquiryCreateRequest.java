package com.corum.backend.dto.inquiry;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class InquiryCreateRequest {

    private String writerName;

    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    private String content;

    private String contactPhone;

    private String contactEmail;

    // INQUIRY / BUG_REPORT / FEATURE_REQUEST
    private String inquiryType = "INQUIRY";
}
