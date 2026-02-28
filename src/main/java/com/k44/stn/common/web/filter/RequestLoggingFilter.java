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

    @Override
    protected void doFilterInternal (
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {


        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;

            String requestId = MDC.get(RequestIdFilter.MDC_KEY);
            if(requestId == null){
                Object attr = request.getAttribute(RequestIdFilter.MDC_KEY);
                requestId = (attr != null) ? String.valueOf(attr) : null;
            }

            String authName = null;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            // TODO(STN-SECURITY-JWT):
            //  Когда будет реализована JWT-аутентификация (spec-domain: JWT HS256, sub=userId),
            //  заменить authName на доменный userId, извлекаемый из JWT claim "sub".
            //  Ожидаемое поведение:
            //    - authentication.getPrincipal() должен содержать userId,
            //    - либо кастомный JwtAuthenticationToken,
            //    - либо собственный UserPrincipal с getUserId().
            if (authentication != null
                    && authentication.isAuthenticated()
                    && !(authentication instanceof AnonymousAuthenticationToken)) {
                authName = authentication.getName();
            }

            int status = response.getStatus();
            String method = request.getMethod();
            String path = request.getRequestURI();

            log.info("request_completed requestId={} principal={} method={} path={} status={} durationMs={}",
                    requestId,
                    authName,
                    method,
                    path,
                    status,
                    durationMs);
        }
    }
}