package com.oxtore.backend.identity.config;

import com.oxtore.backend.identity.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    /** HS512 needs a key of at least 512 bits. */
    private static final int MIN_REFRESH_SECRET_BYTES = 64;
    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String REFRESH_TYPE = "refresh";

    @Value("${jwt.secret:your-secret-key-min-256-bits-long-for-production}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpirationMs;

    /** Separate key: a refresh token can never validate as an access token (JwtAuthFilter uses jwt.secret). */
    @Value("${jwt.refresh-secret:}")
    private String refreshSecret;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

    @PostConstruct
    void validateRefreshSecret() {
        if (refreshSecret == null || refreshSecret.getBytes(StandardCharsets.UTF_8).length < MIN_REFRESH_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_REFRESH_SECRET must be set to a random value of at least " + MIN_REFRESH_SECRET_BYTES + " bytes");
        }
        if (refreshSecret.equals(jwtSecret)) {
            throw new IllegalStateException("JWT_REFRESH_SECRET must differ from JWT_SECRET");
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    private SecretKey getRefreshSigningKey() {
        return Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String email, UserRole role) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /** Long-lived token that can only be exchanged at POST /api/auth/refresh. Carries no role or email. */
    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TYPE)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(getRefreshSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Verifies signature, expiry and type of a refresh token.
     * @throws JwtException (or IllegalArgumentException for blank input) when it isn't a valid refresh token
     */
    public Claims parseRefreshToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getRefreshSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!REFRESH_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM))) {
            throw new JwtException("Not a refresh token");
        }
        return claims;
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long extractUserId(String token) {
        return Long.parseLong(extractClaims(token).getSubject());
    }

    public String extractEmail(String token) {
        return (String) extractClaims(token).get("email");
    }

    public String extractRole(String token) {
        return (String) extractClaims(token).get("role");
    }
}
