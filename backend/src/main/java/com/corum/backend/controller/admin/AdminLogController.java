package com.corum.backend.controller.admin;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.log.AuditLogResponse;
import com.corum.backend.dto.log.SearchLogResponse;
import com.corum.backend.dto.log.SmtpSendLogResponse;
import com.corum.backend.dto.log.VisitLogResponse;
import com.corum.backend.dto.mail.SmtpTestRequest;
import com.corum.backend.service.log.AdminLogService;
import com.corum.backend.service.mail.MailService;
import com.corum.backend.service.mail.MailTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/logs")
public class AdminLogController {

    private final AdminLogService adminLogService;
    private final MailService mailService;
    private final MailTemplateService mailTemplateService;

    @GetMapping("/audit")
    public ApiResponse<Page<AuditLogResponse>> getAuditLogs(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminLogService.getAuditLogs(PageRequest.of(page, size)));
    }

    @GetMapping("/search")
    public ApiResponse<Page<SearchLogResponse>> getSearchLogs(@RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminLogService.getSearchLogs(PageRequest.of(page, size)));
    }

    @GetMapping("/visits")
    public ApiResponse<Page<VisitLogResponse>> getVisitLogs(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminLogService.getVisitLogs(PageRequest.of(page, size)));
    }

    @GetMapping("/smtp")
    public ApiResponse<Page<SmtpSendLogResponse>> getSmtpLogs(@RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminLogService.getSmtpLogs(PageRequest.of(page, size)));
    }

    @PostMapping("/smtp/test")
    public ApiResponse<Void> sendTestMail(@Valid @RequestBody SmtpTestRequest request) {
        String html = mailTemplateService.render(MailTemplateService.ICON_CHECK, MailTemplateService.TINT_BLUE, MailTemplateService.STROKE_BLUE,
                "SMTP 테스트", "이 메일이 보인다면 SMTP 설정이 정상적으로 동작하고 있는 것입니다.", null, null, null);
        mailService.send(null, request.getToEmail(), "[Corum] SMTP 테스트", html, "TEST");
        return ApiResponse.ok("테스트 메일을 발송했습니다.");
    }
}
