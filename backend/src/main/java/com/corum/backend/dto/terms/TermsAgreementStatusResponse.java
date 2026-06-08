package com.corum.backend.dto.terms;

import lombok.Getter;

import java.util.List;

@Getter
public class TermsAgreementStatusResponse {

    private final boolean requiresAgreement;
    private final List<TermsResponse> requiredTerms;

    public TermsAgreementStatusResponse(List<TermsResponse> requiredTerms) {
        this.requiredTerms = requiredTerms;
        this.requiresAgreement = !requiredTerms.isEmpty();
    }
}
