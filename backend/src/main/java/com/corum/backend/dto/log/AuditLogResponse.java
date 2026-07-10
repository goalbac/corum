package com.corum.backend.dto.log;

import com.corum.backend.domain.log.AuditLog;
import com.corum.backend.domain.member.Member;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
public class AuditLogResponse {
    private final Long id;
    private final Long memberId;
    private final String memberUsername;
    private final String memberName;
    private final String actionType;
    private final String targetTable;
    private final Long targetId;
    private final String targetUsername;
    private final String targetName;
    private final String beforeValue;
    private final String afterValue;
    private final String ipAddress;
    private final LocalDateTime createdAt;

    public AuditLogResponse(AuditLog log) {
        this(log, Map.of(), Map.of());
    }

    public AuditLogResponse(AuditLog log, Map<Long, Member> members, Map<Long, Member> targetMembers) {
        this.id = log.getId();
        this.memberId = log.getMemberId();
        Member member = members.get(log.getMemberId());
        this.memberUsername = member != null ? member.getUsername() : null;
        this.memberName = member != null ? member.getName() : null;
        this.actionType = log.getActionType();
        this.targetTable = log.getTargetTable();
        this.targetId = log.getTargetId();
        Member targetMember = targetMembers.get(log.getTargetId());
        this.targetUsername = targetMember != null ? targetMember.getUsername() : null;
        this.targetName = targetMember != null ? targetMember.getName() : null;
        this.beforeValue = log.getBeforeValue();
        this.afterValue = log.getAfterValue();
        this.ipAddress = log.getIpAddress();
        this.createdAt = log.getCreatedAt();
    }
}
