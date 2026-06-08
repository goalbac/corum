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
