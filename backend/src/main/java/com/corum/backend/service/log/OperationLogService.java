package com.corum.backend.service.log;

import com.corum.backend.domain.dashboard.VisitStats;
import com.corum.backend.domain.dashboard.VisitStatsRepository;
import com.corum.backend.domain.log.AuditLog;
import com.corum.backend.domain.log.AuditLogRepository;
import com.corum.backend.domain.log.SearchLog;
import com.corum.backend.domain.log.SearchLogRepository;
import com.corum.backend.domain.log.VisitLog;
import com.corum.backend.domain.log.VisitLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final AuditLogRepository auditLogRepository;
    private final SearchLogRepository searchLogRepository;
    private final VisitLogRepository visitLogRepository;
    private final VisitStatsRepository visitStatsRepository;

    @Transactional
    public void audit(Long memberId, String actionType, String targetTable, Long targetId,
                      String beforeValue, String afterValue, HttpServletRequest request) {
        auditLogRepository.save(AuditLog.builder()
                .memberId(memberId)
                .actionType(actionType)
                .targetTable(targetTable)
                .targetId(targetId)
                .beforeValue(beforeValue)
                .afterValue(afterValue)
                .ipAddress(getClientIp(request))
                .build());
    }

    @Transactional
    public void search(Long memberId, String keyword, String searchType, int resultCount, HttpServletRequest request) {
        if (keyword == null || keyword.isBlank()) return;
        searchLogRepository.save(SearchLog.builder()
                .memberId(memberId)
                .keyword(keyword)
                .searchType(searchType)
                .resultCount(resultCount)
                .ipAddress(getClientIp(request))
                .build());
    }

    @Transactional
    public void visit(Long memberId, HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/api") || uri.equals("/api/health")) return;
        doVisit(memberId, request, uri);
    }

    /** POST /api/page-view 전용 — URI 필터 없이 바로 집계 */
    @Transactional
    public void recordPageView(Long memberId, String pagePath, String previousPath, HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        String pageUri = normalizePagePath(pagePath, referer);
        String previousUri = normalizePagePath(previousPath, null);
        doVisit(memberId, request, pageUri != null ? pageUri : "/", previousUri);
    }

    private void doVisit(Long memberId, HttpServletRequest request, String logUri) {
        doVisit(memberId, request, logUri, null);
    }

    private void doVisit(Long memberId, HttpServletRequest request, String logUri, String refererOverride) {
        LocalDate today = LocalDate.now();
        String ip = getClientIp(request);
        boolean unique = !visitLogRepository.existsByVisitDateAndIpAddress(today, ip);
        boolean loggedIn = memberId != null;

        visitLogRepository.save(VisitLog.builder()
                .memberId(memberId)
                .ipAddress(ip)
                .userAgent(trim(request.getHeader("User-Agent"), 500))
                .requestUri(trim(logUri, 1000))
                .referer(trim(refererOverride != null ? refererOverride : request.getHeader("Referer"), 1000))
                .visitDate(today)
                .build());

        VisitStats stats = visitStatsRepository.findByStatDate(today)
                .orElseGet(() -> visitStatsRepository.save(VisitStats.builder()
                        .statDate(today)
                        .totalVisits(0)
                        .uniqueVisits(0)
                        .loginVisits(0)
                        .build()));
        stats.increase(unique, loggedIn);
    }

    public String getClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String trim(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String normalizePagePath(String pagePath, String referer) {
        if (pagePath != null && !pagePath.isBlank()) {
            String normalized = pagePath.trim();
            return trim(normalized.startsWith("/") ? normalized : "/" + normalized, 1000);
        }
        return trim(referer, 1000);
    }
}
