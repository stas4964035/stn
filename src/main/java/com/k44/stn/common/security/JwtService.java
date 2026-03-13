package com.k44.stn.common.security;


import com.k44.stn.common.error.ErrorCode;
import com.k44.stn.common.error.InvalidJwtException;
import com.k44.stn.common.time.TimeProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

@Service
@AllArgsConstructor
public class JwtService {
    private final JwtProperties jwtProperties;
    private final TimeProvider timeProvider;
    private final SecretKey jwtSigningKey;


    public String generateToken(JwtUserClaims userClaims) {
        Instant now = timeProvider.now();
        Instant expiresAt = now.plus(jwtProperties.ttl());

        return Jwts.builder()
                .subject(String.valueOf(userClaims.userId()))
                .claim("email", userClaims.email())
                .claim("role", userClaims.role())
                .claim("tokenVersion", userClaims.tokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(jwtSigningKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseAndValidate(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(jwtSigningKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidJwtException(ErrorCode.UNAUTHORIZED, "Неверный токен.");
        }
    }

    public Long extractUserId(String token) {
        try {
            Claims claims = parseAndValidate(token);
            return Long.valueOf(claims.getSubject());
        } catch (RuntimeException ex) {
            throw new InvalidJwtException(ErrorCode.UNAUTHORIZED, "Неверный токен.");
        }
    }

    public Long extractTokenVersion(String token) {
        try {
            Claims claims = parseAndValidate(token);

            Object value = claims.get("tokenVersion");
            if (value instanceof Number number) {
                return number.longValue();
            }
            return Long.valueOf(Objects.requireNonNull(value, "Требуется объявление tokenVersion").toString());
        } catch (RuntimeException ex) {
            throw new InvalidJwtException(ErrorCode.UNAUTHORIZED, "Неверный токен.");
        }

    }

    public String extractEmail(String token) {
        return parseAndValidate(token).get("email", String.class);
    }

    public String extractRole(String token) {
        return parseAndValidate(token).get("role", String.class);
    }

}
