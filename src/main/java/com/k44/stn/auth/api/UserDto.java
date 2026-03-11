package com.k44.stn.auth.api;

import java.time.Instant;

public record UserDto(
        Long id,
        String email,
        String nickname,
        String accountStatus,
        String role,
        String avatarIcon,
        boolean isAlive,
        Instant createdAt,
        Instant updatedAt
) {
}
