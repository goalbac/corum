package com.corum.backend.service.auth;

import com.corum.backend.domain.auth.InvalidatedToken;
import com.corum.backend.domain.auth.InvalidatedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 강제 로그아웃/계정 잠금/비밀번호 재설정 등으로 무효화된 토큰을 관리한다.
 * 무효화 목록(invalidatedTokens)은 DB(invalidated_tokens)에 저장해 서버 재시작에도 유지된다.
 * 동시 로그인 제한용 activeTokens는 인메모리로 유지 — 재시작 시 초기화돼도
 * 다음 로그인부터 정상 동작하는 부가 기능이라 영속화 대상에서 제외했다.
 */
@Service
@RequiredArgsConstructor
public class TokenSessionService {

    // 토큰 만료 시간을 파싱하지 않고, 무효화된 토큰은 넉넉하게 최대 유효기간(24시간) 뒤 정리한다.
    private static final long BLACKLIST_RETENTION_HOURS = 24;

    private final InvalidatedTokenRepository invalidatedTokenRepository;
    private final ConcurrentHashMap<Long, String> activeTokens = new ConcurrentHashMap<>();

    public void registerLogin(Long memberId, String token, boolean allowConcurrentLogin) {
        if (!allowConcurrentLogin) {
            String previousToken = activeTokens.put(memberId, token);
            if (previousToken != null && !previousToken.equals(token)) {
                invalidateToken(previousToken);
            }
            return;
        }
        activeTokens.put(memberId, token);
    }

    public void invalidateMember(Long memberId) {
        String token = activeTokens.remove(memberId);
        if (token != null) {
            invalidateToken(token);
        }
    }

    @Transactional
    public void invalidateToken(String token) {
        if (token == null || token.isBlank()) return;
        String hash = hash(token);
        if (invalidatedTokenRepository.existsById(hash)) return;
        invalidatedTokenRepository.save(InvalidatedToken.builder()
                .tokenHash(hash)
                .expiresAt(LocalDateTime.now().plusHours(BLACKLIST_RETENTION_HOURS))
                .build());
    }

    @Transactional(readOnly = true)
    public boolean isInvalidated(String token) {
        if (token == null || token.isBlank()) return false;
        return invalidatedTokenRepository.existsById(hash(token));
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
