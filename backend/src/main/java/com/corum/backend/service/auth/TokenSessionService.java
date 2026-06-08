package com.corum.backend.service.auth;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenSessionService {

    private final ConcurrentHashMap<Long, String> activeTokens = new ConcurrentHashMap<>();
    private final Set<String> invalidatedTokens = ConcurrentHashMap.newKeySet();

    public void registerLogin(Long memberId, String token, boolean allowConcurrentLogin) {
        if (!allowConcurrentLogin) {
            String previousToken = activeTokens.put(memberId, token);
            if (previousToken != null && !previousToken.equals(token)) {
                invalidatedTokens.add(previousToken);
            }
            return;
        }
        activeTokens.put(memberId, token);
    }

    public void invalidateMember(Long memberId) {
        String token = activeTokens.remove(memberId);
        if (token != null) {
            invalidatedTokens.add(token);
        }
    }

    public void invalidateToken(String token) {
        if (token != null && !token.isBlank()) {
            invalidatedTokens.add(token);
        }
    }

    public boolean isInvalidated(String token) {
        return invalidatedTokens.contains(token);
    }
}
