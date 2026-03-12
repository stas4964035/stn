package com.k44.stn.auth.application;

import com.k44.stn.auth.api.UserDto;
import com.k44.stn.common.error.ConflictException;
import com.k44.stn.common.error.ErrorCode;
import com.k44.stn.common.security.JwtService;
import com.k44.stn.users.persistence.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthApplicationService {

    public UserDto register(String email, String rawPassword, String nickname){
        if(userRepository.existsByEmail(email)) {
            throw new ConflictException(ErrorCode.CONFLICT, "Пользователь с таким email уже зарегистрирован");
            return null;
        }
        String hashedPassword = passwordEncoder.encode(rawPassword);
        String
    }

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;
}
