package com.corum.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 브루트포스/스팸 방어용 최소한의 IP 기반 rate limiting.
 * 단일 인스턴스 운영을 전제로 한 인메모리 구현(다중 인스턴스 스케일아웃 시에는
 * Redis 등 공유 저장소로 교체 필요) — 로그인 실패 잠금과 별개로 분산 요청 자체를 제한한다.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private record Limit(int maxRequests, long windowMs) {}

    private static final Map<String, Limit> LIMITS = Map.of(
            "POST:/api/auth/login", new Limit(10, 5 * 60_000L),
            "POST:/api/auth/register", new Limit(5, 60 * 60_000L),
            "POST:/api/auth/signup", new Limit(5, 60 * 60_000L),
            "POST:/api/auth/request-password-reset", new Limit(5, 30 * 60_000L),
            "POST:/api/inquiries", new Limit(5, 10 * 60_000L)
    );

    private static final long MAX_WINDOW_MS = 60 * 60_000L;
    private static final int CLEANUP_THRESHOLD = 50_000;

    private record WindowState(long windowStart, int count) {}

    private final ConcurrentHashMap<String, WindowState> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Limit limit = LIMITS.get(request.getMethod() + ":" + request.getRequestURI());
        if (limit == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String bucketKey = request.getMethod() + ":" + request.getRequestURI() + ":" + resolveIp(request);
        if (!tryConsume(bucketKey, limit)) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"success\":false,\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean tryConsume(String key, Limit limit) {
        long now = System.currentTimeMillis();
        if (buckets.size() > CLEANUP_THRESHOLD) {
            buckets.entrySet().removeIf(e -> now - e.getValue().windowStart() >= MAX_WINDOW_MS);
        }
        boolean[] allowed = new boolean[1];
        buckets.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart() >= limit.windowMs()) {
                allowed[0] = true;
                return new WindowState(now, 1);
            }
            if (existing.count() < limit.maxRequests()) {
                allowed[0] = true;
                return new WindowState(existing.windowStart(), existing.count() + 1);
            }
            allowed[0] = false;
            return existing;
        });
        return allowed[0];
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
