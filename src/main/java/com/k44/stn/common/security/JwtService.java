package com.k44.stn.common.security;

import com.k44.stn.common.error.ConflictException;
import com.k44.stn.common.error.ErrorCode;
import com.k44.stn.common.error.InvalidJwtException;
import com.k44.stn.common.time.TimeProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Service
@AllArgsConstructor
public class JwtService {
    private final JwtProperties jwtProperties;
    private final TimeProvider timeProvider;

    private SecretKey signingKey;

    @PostConstruct
    void init(){
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(JwtUserClaims userClaims){
        Instant now = timeProvider.now();
        Instant expiresAt = now.plus(jwtProperties.ttl());

        return Jwts.builder()
                .subject(String.valueOf(userClaims.userId()))
                .claim("email", userClaims.email())
                .claim("role", userClaims.role())
                .claim("tokenVersion", userClaims.tokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseAndValidate(String token){
        try{
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        }catch (JwtException | IllegalArgumentException ex){
            throw new InvalidJwtException(ErrorCode.VALIDATION_ERROR, "Неверный токен.");
        }
    }

    public Long extractUserId(String token){
        Claims claims = parseAndValidate(token);
        return Long.valueOf(claims.getSubject());
    }

    public Long extractTokenVersion(String token){
        Claims claims = parseAndValidate(token);
        Object value = claims.get("tokenVersion");
        if(value instanceof Number number){
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    public String extractEmail(String token){
        return parseAndValidate(token).get("email", String.class);
    }

    public String ExtractRole(String token){
        return parseAndValidate(token).get("role", String.class);
    }

}
