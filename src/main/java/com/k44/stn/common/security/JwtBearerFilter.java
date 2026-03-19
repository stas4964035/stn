package com.k44.stn.common.security;

import com.k44.stn.auth.application.RedisTokenVersionService;
import com.k44.stn.common.error.ErrorCode;
import com.k44.stn.common.error.InvalidJwtException;
import com.k44.stn.common.error.NotFoundException;
import com.k44.stn.common.error.UnauthorizedException;
import com.k44.stn.users.domain.AccountStatus;
import com.k44.stn.users.domain.User;
import com.k44.stn.users.persistence.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpHeaders;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtBearerFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if(authorization == null || !authorization.startsWith("Bearer ")){
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7).trim();
        if(token.isEmpty()){
            filterChain.doFilter(request, response);
            return;
        }

        try {
            JwtUserClaims claims = jwtService.parseAndValidate(token);

            if (claims.tokenVersion() != tokenVersionService.getCurrent(claims.userId())){
                throw new InvalidJwtException(ErrorCode.UNAUTHORIZED, "Неверная версия токена");
            }

            User user = userRepository.findById(claims.userId()).orElseThrow(() -> new InvalidJwtException(ErrorCode.UNAUTHORIZED, "Неверный токен"));

            if(user.getAccountStatus() == AccountStatus.BLOCKED){
                throw new UnauthorizedException(ErrorCode.ACCOUNT_BLOCKED, "Аккаунт заблокирован");
            }

            if(user.getAccountStatus() == AccountStatus.DELETED){
                throw new UnauthorizedException(ErrorCode.ACCOUNT_DELETED, "Аккаунт удален");
            }

            UserPrincipal userPrincipal = new UserPrincipal(user.getId(), user.getEmail(), user.getSystemRole().name());

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.authorities());

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (UnauthorizedException | InvalidJwtException ex){
            SecurityContextHolder.clearContext();
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED, "Ошибка авторизации");
        }
    }


    private final UserRepository userRepository;
    private final RedisTokenVersionService tokenVersionService;
    private final JwtService jwtService;
}
