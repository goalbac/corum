package com.corum.backend.config;

import com.corum.backend.security.JwtProvider;
import com.corum.backend.service.log.OperationLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * SPA 페이지 단위 방문 집계 필터.
 * 프론트엔드가 라우터 이동 시 X-Page-View: 1 헤더를 포함해 보낸 요청만 집계한다.
 * (모든 API 호출을 집계하면 한 페이지 로드가 수십 건으로 부풀려짐)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisitLoggingFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final OperationLogService operationLogService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(request, response);

        // X-Page-View 헤더가 없는 일반 API 호출은 집계하지 않음
        if (!"1".equals(request.getHeader("X-Page-View"))) return;

        // 관리자 경로는 별도 집계 제외
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/api/admin")) return;

        try {
            operationLogService.visit(resolveMemberId(request), request);
        } catch (Exception e) {
            log.debug("Visit logging failed: {}", e.getMessage());
        }
    }

    private Long resolveMemberId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        String token = header.substring(7);
        return jwtProvider.validate(token) ? jwtProvider.getMemberId(token) : null;
    }
}
