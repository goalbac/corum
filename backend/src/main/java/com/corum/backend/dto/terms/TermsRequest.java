package com.corum.backend.dto.terms;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TermsRequest {

    @NotBlank(message = "약관 유형을 입력해주세요.")
    private String type;

    private String content;
    private Boolean isActive = true;
    private Boolean requireReagree = false;
}
