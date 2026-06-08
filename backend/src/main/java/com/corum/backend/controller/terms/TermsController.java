package com.corum.backend.controller.terms;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.terms.TermsAgreementRequest;
import com.corum.backend.dto.terms.TermsAgreementStatusResponse;
import com.corum.backend.dto.terms.TermsResponse;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.terms.TermsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermsController {

    private final TermsService termsService;

    @GetMapping("/active")
    public ApiResponse<List<TermsResponse>> activeTerms() {
        return ApiResponse.ok(termsService.getActiveTerms());
    }

    @GetMapping("/required")
    public ApiResponse<TermsAgreementStatusResponse> requiredTerms(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(termsService.getAgreementStatus(userDetails.getMemberId()));
    }

    @PostMapping("/agreements")
    public ApiResponse<Void> agree(@AuthenticationPrincipal CustomUserDetails userDetails,
                                   @Valid @RequestBody TermsAgreementRequest request,
                                   HttpServletRequest httpRequest) {
        termsService.agree(userDetails.getMemberId(), request.getTermsIds(), httpRequest);
        return ApiResponse.ok("약관 동의가 저장되었습니다.");
    }
}
