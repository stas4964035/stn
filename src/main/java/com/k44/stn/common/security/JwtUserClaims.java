package com.k44.stn.common.security;

import com.k44.stn.users.domain.User;

public record JwtUserClaims(
        Long userId,
        String email,
        String role,
        Long tokenVersion
) {
    public static JwtUserClaims from(User user, long tokenVersion){
        return new JwtUserClaims(
                user.getId(),
                user.getEmail(),
                user.getSystemRole().name(),
                tokenVersion
        );
    }
}
