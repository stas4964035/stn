package com.k44.stn.common.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private String resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return "anonymous";
        }


        // TODO(STN-SECURITY-JWT):
        //  Когда будет реализована JWT-аутентификация (spec-domain: JWT HS256, sub=userId),
        //  заменить principal на доменный userId, извлекаемый из JWT claim "sub".
        //  Ожидаемое поведение:
        //    - authentication.getPrincipal() должен содержать userId,
        //    - либо кастомный JwtAuthenticationToken,
        //    - либо собственный UserPrincipal с getUserId().
        return authentication.getName();

    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {


        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;

            String userId = resolveUserId();
            String method = request.getMethod();
            String path = request.getRequestURI();
            int status = response.getStatus();

            log.info("HTTP {} {} -> {} durationMs={} userId={}",
                    method,
                    path,
                    status,
                    durationMs,
                    userId
            );
        }
    }
}