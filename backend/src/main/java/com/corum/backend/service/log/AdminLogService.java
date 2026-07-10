package com.corum.backend.service.log;

import com.corum.backend.domain.log.AuditLogRepository;
import com.corum.backend.domain.log.SearchLogRepository;
import com.corum.backend.domain.log.SmtpSendLogRepository;
import com.corum.backend.domain.log.VisitLogRepository;
import com.corum.backend.domain.member.Member;
import com.corum.backend.domain.member.MemberRepository;
import com.corum.backend.dto.log.AuditLogResponse;
import com.corum.backend.dto.log.SearchLogResponse;
import com.corum.backend.dto.log.SmtpSendLogResponse;
import com.corum.backend.dto.log.VisitLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminLogService {

    private final AuditLogRepository auditLogRepository;
    private final SearchLogRepository searchLogRepository;
    private final VisitLogRepository visitLogRepository;
    private final SmtpSendLogRepository smtpSendLogRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
        var logs = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        Set<Long> memberIds = logs.getContent().stream()
                .map(log -> log.getMemberId())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Set<Long> targetMemberIds = logs.getContent().stream()
                .filter(log -> "members".equals(log.getTargetTable()))
                .map(log -> log.getTargetId())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, Member> members = findMembers(memberIds);
        Map<Long, Member> targetMembers = findMembers(targetMemberIds);
        return logs.map(log -> new AuditLogResponse(log, members, targetMembers));
    }

    @Transactional(readOnly = true)
    public Page<SearchLogResponse> getSearchLogs(Pageable pageable) {
        var logs = searchLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        Map<Long, Member> members = findMembers(logs.getContent().stream()
                .map(log -> log.getMemberId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return logs.map(log -> new SearchLogResponse(log, members));
    }

    @Transactional(readOnly = true)
    public Page<VisitLogResponse> getVisitLogs(Pageable pageable) {
        var logs = visitLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        Map<Long, Member> members = findMembers(logs.getContent().stream()
                .map(log -> log.getMemberId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return logs.map(log -> new VisitLogResponse(log, members));
    }

    @Transactional(readOnly = true)
    public Page<SmtpSendLogResponse> getSmtpLogs(Pageable pageable) {
        var logs = smtpSendLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        Map<Long, Member> members = findMembers(logs.getContent().stream()
                .map(log -> log.getMemberId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return logs.map(log -> new SmtpSendLogResponse(log, members));
    }

    private Map<Long, Member> findMembers(Set<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return memberRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));
    }
}
