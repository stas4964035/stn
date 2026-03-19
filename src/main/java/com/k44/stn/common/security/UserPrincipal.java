package com.k44.stn.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public record UserPrincipal(
        Long userId,
        String email,
        String role
) implements Serializable {
    public Collection<? extends GrantedAuthority> authorities(){
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
