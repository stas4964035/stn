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

    public JwtUserClaims parseAndValidate(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(jwtSigningKey).build().parseSignedClaims(token).getPayload();
            Long userId = Long.valueOf(claims.getSubject());
            String email = claims.get("email", String.class);
            String role = claims.get("role", String.class);
            Long tokenVersion = claims.get("tokenVersion", Long.class);
            return new JwtUserClaims(userId, email, role, tokenVersion);
        } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
            throw new InvalidJwtException(ErrorCode.UNAUTHORIZED, "Неверный токен.");
        }
    }

    private final JwtProperties jwtProperties;
    private final TimeProvider timeProvider;
    private final SecretKey jwtSigningKey;


}
