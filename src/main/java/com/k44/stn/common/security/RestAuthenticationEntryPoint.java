package com.k44.stn.common.security;

import com.k44.stn.common.error.ErrorCode;
import com.k44.stn.common.error.ErrorResponse;
import com.k44.stn.common.error.ErrorResponseFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ErrorResponse body = factory.rest(
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                "Не авторизирован",
                request,
                Map.of()
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json; charset=utf-8");
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper, ErrorResponseFactory factory) {
        this.objectMapper = objectMapper;
        this.factory = factory;
    }
    private final ObjectMapper objectMapper;

    private final ErrorResponseFactory factory;
}
