package com.corum.backend.controller;

import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.log.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PageViewController {

    private final OperationLogService operationLogService;

    @PostMapping("/page-view")
    public ResponseEntity<Void> record(
            @RequestBody(required = false) PageViewRequest body,
            HttpServletRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        operationLogService.recordPageView(
                memberId,
                body != null ? body.pagePath() : null,
                body != null ? body.previousPath() : null,
                request
        );
        return ResponseEntity.ok().build();
    }

    public record PageViewRequest(String pagePath, String previousPath, String pageTitle) {
    }
}
