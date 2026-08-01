package com.opportunityboard.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms:900000}") // 15 phút
    private long expirationMs;

    @Value("${app.jwt.refresh-ms:1209600000}") // 14 ngày
    private long refreshMs;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, String role, int passwordVersion) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .claim("pv", passwordVersion) // revoke khi đổi mk
                .claim("typ", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(getKey())
                .compact();
    }

    public String generateRefreshToken(UUID userId, int passwordVersion) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("pv", passwordVersion)
                .claim("typ", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshMs))
                .signWith(getKey())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token).getPayload();
    }

    public UUID getUserId(String token) {
        return UUID.fromString(parse(token).getSubject());
    }

    public int getPasswordVersion(String token) {
        return ((Number) parse(token).get("pv")).intValue();
    }

    public boolean isRefresh(String token) {
        return "refresh".equals(parse(token).get("typ"));
    }

    public long getRemainingMs(String token) {
        return parse(token).getExpiration().getTime() - System.currentTimeMillis();
    }
}
