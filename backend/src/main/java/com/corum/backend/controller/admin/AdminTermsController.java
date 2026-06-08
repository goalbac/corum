package com.corum.backend.controller.admin;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.terms.TermsRequest;
import com.corum.backend.dto.terms.TermsResponse;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.terms.TermsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/terms")
@RequiredArgsConstructor
public class AdminTermsController {

    private final TermsService termsService;

    @GetMapping
    public ApiResponse<List<TermsResponse>> getTerms() {
        return ApiResponse.ok(termsService.getAll());
    }

    @PostMapping
    public ApiResponse<TermsResponse> createTerms(@Valid @RequestBody TermsRequest request,
                                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                                  HttpServletRequest httpRequest) {
        return ApiResponse.ok(termsService.createVersion(request, userDetails.getMemberId(), httpRequest));
    }

    @PutMapping("/{id}")
    public ApiResponse<TermsResponse> updateTerms(@PathVariable Long id,
                                                  @Valid @RequestBody TermsRequest request,
                                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                                  HttpServletRequest httpRequest) {
        return ApiResponse.ok(termsService.update(id, request, userDetails.getMemberId(), httpRequest));
    }

    @PostMapping("/{id}/activate")
    public ApiResponse<TermsResponse> activate(@PathVariable Long id,
                                               @AuthenticationPrincipal CustomUserDetails userDetails,
                                               HttpServletRequest httpRequest) {
        return ApiResponse.ok(termsService.activate(id, userDetails.getMemberId(), httpRequest));
    }
}
