package com.corum.backend.service.log;

import com.corum.backend.domain.log.AuditLogRepository;
import com.corum.backend.domain.log.SearchLogRepository;
import com.corum.backend.domain.log.SmtpSendLogRepository;
import com.corum.backend.domain.log.VisitLogRepository;
import com.corum.backend.dto.log.AuditLogResponse;
import com.corum.backend.dto.log.SearchLogResponse;
import com.corum.backend.dto.log.SmtpSendLogResponse;
import com.corum.backend.dto.log.VisitLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminLogService {

    private final AuditLogRepository auditLogRepository;
    private final SearchLogRepository searchLogRepository;
    private final VisitLogRepository visitLogRepository;
    private final SmtpSendLogRepository smtpSendLogRepository;

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable).map(AuditLogResponse::new);
    }

    @Transactional(readOnly = true)
    public Page<SearchLogResponse> getSearchLogs(Pageable pageable) {
        return searchLogRepository.findAllByOrderByCreatedAtDesc(pageable).map(SearchLogResponse::new);
    }

    @Transactional(readOnly = true)
    public Page<VisitLogResponse> getVisitLogs(Pageable pageable) {
        return visitLogRepository.findAllByOrderByCreatedAtDesc(pageable).map(VisitLogResponse::new);
    }

    @Transactional(readOnly = true)
    public Page<SmtpSendLogResponse> getSmtpLogs(Pageable pageable) {
        return smtpSendLogRepository.findAllByOrderByCreatedAtDesc(pageable).map(SmtpSendLogResponse::new);
    }
}
