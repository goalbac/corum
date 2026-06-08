package com.corum.backend.dto.log;

import com.corum.backend.domain.log.AuditLog;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AuditLogResponse {
    private final Long id;
    private final Long memberId;
    private final String actionType;
    private final String targetTable;
    private final Long targetId;
    private final String beforeValue;
    private final String afterValue;
    private final String ipAddress;
    private final LocalDateTime createdAt;

    public AuditLogResponse(AuditLog log) {
        this.id = log.getId();
        this.memberId = log.getMemberId();
        this.actionType = log.getActionType();
        this.targetTable = log.getTargetTable();
        this.targetId = log.getTargetId();
        this.beforeValue = log.getBeforeValue();
        this.afterValue = log.getAfterValue();
        this.ipAddress = log.getIpAddress();
        this.createdAt = log.getCreatedAt();
    }
}
