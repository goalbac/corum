package com.corum.backend.dto.log;

import com.corum.backend.domain.log.VisitLog;
import com.corum.backend.domain.member.Member;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
public class VisitLogResponse {
    private final Long id;
    private final Long memberId;
    private final String memberUsername;
    private final String memberName;
    private final String ipAddress;
    private final String userAgent;
    private final String requestUri;
    private final String referer;
    private final LocalDate visitDate;
    private final LocalDateTime createdAt;

    public VisitLogResponse(VisitLog log) {
        this(log, Map.of());
    }

    public VisitLogResponse(VisitLog log, Map<Long, Member> members) {
        this.id = log.getId();
        this.memberId = log.getMemberId();
        Member member = members.get(log.getMemberId());
        this.memberUsername = member != null ? member.getUsername() : null;
        this.memberName = member != null ? member.getName() : null;
        this.ipAddress = log.getIpAddress();
        this.userAgent = log.getUserAgent();
        this.requestUri = log.getRequestUri();
        this.referer = log.getReferer();
        this.visitDate = log.getVisitDate();
        this.createdAt = log.getCreatedAt();
    }
}
