package com.corum.backend.dto.log;

import com.corum.backend.domain.log.SmtpSendLog;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SmtpSendLogResponse {
    private final Long id;
    private final Long memberId;
    private final String toEmail;
    private final String subject;
    private final String sendType;
    private final Boolean success;
    private final String errorMessage;
    private final LocalDateTime createdAt;

    public SmtpSendLogResponse(SmtpSendLog log) {
        this.id = log.getId();
        this.memberId = log.getMemberId();
        this.toEmail = log.getToEmail();
        this.subject = log.getSubject();
        this.sendType = log.getSendType();
        this.success = log.getSuccess();
        this.errorMessage = log.getErrorMessage();
        this.createdAt = log.getCreatedAt();
    }
}
