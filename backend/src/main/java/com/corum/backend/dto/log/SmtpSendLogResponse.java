package com.corum.backend.dto.log;

import com.corum.backend.domain.log.SmtpSendLog;
import com.corum.backend.domain.member.Member;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
public class SmtpSendLogResponse {
    private final Long id;
    private final Long memberId;
    private final String memberUsername;
    private final String memberName;
    private final String toEmail;
    private final String subject;
    private final String sendType;
    private final Boolean success;
    private final String errorMessage;
    private final LocalDateTime createdAt;

    public SmtpSendLogResponse(SmtpSendLog log) {
        this(log, Map.of());
    }

    public SmtpSendLogResponse(SmtpSendLog log, Map<Long, Member> members) {
        this.id = log.getId();
        this.memberId = log.getMemberId();
        Member member = members.get(log.getMemberId());
        this.memberUsername = member != null ? member.getUsername() : null;
        this.memberName = member != null ? member.getName() : null;
        this.toEmail = log.getToEmail();
        this.subject = log.getSubject();
        this.sendType = log.getSendType();
        this.success = log.getSuccess();
        this.errorMessage = log.getErrorMessage();
        this.createdAt = log.getCreatedAt();
    }
}
