package com.corum.backend.dto.log;

import com.corum.backend.domain.log.VisitLog;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class VisitLogResponse {
    private final Long id;
    private final Long memberId;
    private final String ipAddress;
    private final String requestUri;
    private final String referer;
    private final LocalDate visitDate;
    private final LocalDateTime createdAt;

    public VisitLogResponse(VisitLog log) {
        this.id = log.getId();
        this.memberId = log.getMemberId();
        this.ipAddress = log.getIpAddress();
        this.requestUri = log.getRequestUri();
        this.referer = log.getReferer();
        this.visitDate = log.getVisitDate();
        this.createdAt = log.getCreatedAt();
    }
}
