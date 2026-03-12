package com.k44.stn.auth.api;

import com.k44.stn.users.domain.User;

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
    public static UserDto from(User user){
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getAccountStatus().name(),
                user.getSystemRole().name(),
                user.getAvatarIcon(),
                user.isAlive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
