package com.k44.stn.auth.application;

import com.k44.stn.auth.api.AuthResponse;
import com.k44.stn.auth.api.LoginRequest;
import com.k44.stn.auth.api.RegisterRequest;
import com.k44.stn.auth.api.UserDto;
import com.k44.stn.auth.domain.TokenVersionService;
import com.k44.stn.common.error.*;
import com.k44.stn.common.security.JwtService;
import com.k44.stn.common.security.JwtUserClaims;
import com.k44.stn.users.domain.AccountStatus;
import com.k44.stn.users.domain.User;
import com.k44.stn.users.persistence.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class AuthService {

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(ErrorCode.USER_ALREADY_EXISTS, "Пользователь с таким email уже зарегистрирован");
        }
        String hashedPassword = passwordEncoder.encode(request.password());

        User user = User.create(request.email(), hashedPassword, request.nickname());
        User savedUser = userRepository.save(user);

        long tokenVersion = tokenVersionService.initializeAndGet(savedUser.getId());
        String token = jwtService.generateToken(JwtUserClaims.from(savedUser, tokenVersion));

        UserDto userDto = UserDto.from(savedUser);

        return new AuthResponse(token, userDto);
    }

    public AuthResponse login(LoginRequest request){

        User user = userRepository.findByEmail(request.email()).orElseThrow(()-> new NotFoundException(ErrorCode.INVALID_CREDENTIALS, "Пользователь с таким email не зарегистрирован"));
        if(!passwordEncoder.matches(request.password(), user.getPasswordHash())) throw new InvalidCredentialsException(ErrorCode.INVALID_CREDENTIALS, "Неверный пароль");
        if(user.getAccountStatus() == AccountStatus.BLOCKED) throw new ForbiddenException(ErrorCode.ACCOUNT_BLOCKED, "Аккаунт заблокирован");
        if(user.getAccountStatus() == AccountStatus.DELETED) throw new ForbiddenException(ErrorCode.ACCOUNT_DELETED, "Аккаунт удален");
        long tokenVersion = tokenVersionService.getCurrent(user.getId());
        String token = jwtService.generateToken(JwtUserClaims.from(user, tokenVersion));

        UserDto userDto = UserDto.from(user);

        return new AuthResponse(token, userDto);
    }

    private final TokenVersionService tokenVersionService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;
}
