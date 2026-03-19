package com.sancanji.mealsapi.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String openId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("openId", openId);
        return createToken(claims);
    }

    private String createToken(Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .signWith(getSecretKey())
                .compact();
    }

    public Claims parseToken(String token) {
        if (token == null || token.trim().isEmpty() || "null".equals(token) || "undefined".equals(token)) {
            throw new IllegalArgumentException("Token is null or empty");
        }
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    public boolean isTokenValid(String token) {
        if (token == null || token.trim().isEmpty() || "null".equals(token) || "undefined".equals(token)) {
            return false;
        }
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}