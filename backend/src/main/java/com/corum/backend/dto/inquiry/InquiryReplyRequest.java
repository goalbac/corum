package com.corum.backend.dto.inquiry;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InquiryReplyRequest {

    @NotBlank(message = "답변 내용을 입력해주세요.")
    private String content;
}
