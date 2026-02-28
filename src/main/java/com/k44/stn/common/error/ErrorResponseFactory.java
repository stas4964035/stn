package com.k44.stn.common.error;


import com.k44.stn.common.time.TimeProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ErrorResponseFactory {
    private final TimeProvider timeProvider;

    public ErrorResponseFactory(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    public ErrorResponse rest(HttpStatus status,
                              ErrorCode code,
                              String message,
                              HttpServletRequest request,
                              Map<String, Object> details){
        return new ErrorResponse(
                timeProvider.now(),
                status.value(),
                code.name(),
                message,
                request.getRequestURI(),
                details == null ? Map.of() : details
        );
    }

    public ErrorResponse ws(HttpStatus status,
                            ErrorCode code,
                            String message,
                            Map<String, Object> details){
        return new ErrorResponse(
                timeProvider.now(),
                status.value(),
                code.name(),
                message,
                "/ws/events",
                details == null ? Map.of() : details
        );
    }
}
