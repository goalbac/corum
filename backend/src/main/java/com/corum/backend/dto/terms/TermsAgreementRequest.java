package com.corum.backend.dto.terms;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TermsAgreementRequest {

    @NotEmpty(message = "동의할 약관을 선택해주세요.")
    private List<Long> termsIds;
}
