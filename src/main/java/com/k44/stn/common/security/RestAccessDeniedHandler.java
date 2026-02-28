package com.k44.stn.common.security;

import com.k44.stn.common.error.ErrorCode;
import com.k44.stn.common.error.ErrorResponse;
import com.k44.stn.common.error.ErrorResponseFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.graalvm.nativeimage.ObjectHandle;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ErrorResponse body = factory.rest(
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN,
                "Доступ запрещен",
                request,
                Map.of()
        );
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json; charset=utf-8");
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private final ObjectMapper objectMapper;
    private final ErrorResponseFactory factory;

    public RestAccessDeniedHandler(ObjectMapper objectMapper, ErrorResponseFactory factory) {
        this.objectMapper = objectMapper;
        this.factory = factory;
    }
}
