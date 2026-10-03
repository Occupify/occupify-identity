package com.occupify.identity.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_ROLE = "role";
    public static final int HS512_MIN_KEY_BYTES = 64;

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtUtils(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration:3600000}") long accessTokenExpirationMs,
            @Value("${app.jwt.refresh-token-expiration:604800000}") long refreshTokenExpirationMs) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < HS512_MIN_KEY_BYTES) {
            throw new IllegalArgumentException("JWT secret key must be at least " + HS512_MIN_KEY_BYTES + " bytes (512 bits) long for HS512.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    /**
     * Generates an Access Token with claims: userId, role, sub (email).
     */
    public String generateAccessToken(String email, String userId, String role) {
        validateNonBlank(email, "Subject email");
        validateNonBlank(userId, "User ID");
        validateNonBlank(role, "User role");

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Generates a Refresh Token with subject (email) and extended expiration.
     */
    public String generateRefreshToken(String email) {
        validateNonBlank(email, "Subject email");

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Parses and validates cryptographic signature, returning token Claims payload.
     */
    public Claims parseClaims(String token) {
        validateNonBlank(token, "JWT token");
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Parses token claims safely, returning Optional.empty() if invalid or expired.
     */
    public java.util.Optional<Claims> parseClaimsIfValid(String token) {
        if (token == null || token.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(parseClaims(token));
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Malformed or invalid JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return java.util.Optional.empty();
    }

    /**
     * Single-pass extraction of UserClaims (userId, email, role) to avoid repeated parsing.
     */
    public java.util.Optional<UserClaims> extractUserClaims(String token) {
        return parseClaimsIfValid(token).map(claims -> new UserClaims(
                claims.get(CLAIM_USER_ID, String.class),
                claims.getSubject(),
                claims.get(CLAIM_ROLE, String.class)
        ));
    }

    /**
     * Validates token signature and expiration without throwing exceptions.
     */
    public boolean validateToken(String token) {
        return parseClaimsIfValid(token).isPresent();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractUserId(String token) {
        return parseClaims(token).get(CLAIM_USER_ID, String.class);
    }

    public String extractRole(String token) {
        return parseClaims(token).get(CLAIM_ROLE, String.class);
    }

    private void validateNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank.");
        }
    }
}
