package com.example.query_service.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret:#{null}}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600000}") // 1 hora por defecto
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters long");
        }
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            logger.warn("Token is null or empty");
            return false;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // ✅ Validación explícita de expiración
            if (claims.getExpiration().before(new Date())) {
                logger.warn("Token has expired");
                return false;
            }

            // ✅ Validación adicional del subject
            if (claims.getSubject() == null || claims.getSubject().trim().isEmpty()) {
                logger.warn("Token has no valid subject");
                return false;
            }

            return true;

        } catch (ExpiredJwtException e) {
            logger.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.warn("JWT token is malformed: {}", e.getMessage());
        } catch (SecurityException e) {
            logger.warn("JWT signature validation failed: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("JWT token compact is invalid: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error validating JWT token", e);
        }

        return false;
    }

    public String extractUsername(String token) {
        try {
            return extractAllClaims(token).getSubject();
        } catch (Exception e) {
            logger.error("Error extracting username from token", e);
            return null;
        }
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extrae los roles del token JWT (si existen)
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return (List<String>) claims.get("roles");
        } catch (Exception e) {
            logger.warn("Error extracting roles from token, using empty list", e);
            return java.util.Collections.emptyList();
        }
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }
}