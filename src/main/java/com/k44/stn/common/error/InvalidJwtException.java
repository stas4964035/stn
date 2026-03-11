package com.k44.stn.common.error;

import org.springframework.http.HttpStatus;

public class InvalidJwtException extends ApiException {
    public InvalidJwtException(ErrorCode code, String message) {
        super(HttpStatus.UNAUTHORIZED, code, message);
    }
}
