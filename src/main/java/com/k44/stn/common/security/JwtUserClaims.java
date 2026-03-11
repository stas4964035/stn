package com.k44.stn.common.security;

public record JwtUserClaims(
        Long userId,
        String email,
        String role,
        Long tokenVersion
) {
}
