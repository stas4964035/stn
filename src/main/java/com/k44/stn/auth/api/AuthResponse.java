package com.k44.stn.auth.api;

import com.k44.stn.users.domain.User;

public record AuthResponse(
        String token,
        User user
) {
}
