package com.corum.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long memberId, String username) {
        return createAccessToken(memberId, username, accessTokenExpiryMs);
    }

    public String createAccessToken(Long memberId, String username, long expiryMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMs))
                .signWith(key)
                .compact();
    }

    public String createPurposeToken(Long memberId, String username, String purpose, long expiryMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("username", username)
                .claim("purpose", purpose)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMs))
                .signWith(key)
                .compact();
    }

    public boolean validate(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
        } catch (JwtException e) {
            log.debug("JWT invalid: {}", e.getMessage());
        }
        return false;
    }

    public Long getMemberId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    public String getUsername(String token) {
        return getClaims(token).get("username", String.class);
    }

    public String getPurpose(String token) {
        return getClaims(token).get("purpose", String.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
