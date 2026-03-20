package com.k44.stn.common.error;

import org.springframework.http.HttpStatus;

public final class InvalidCredentialsException extends ApiException {
    public InvalidCredentialsException(ErrorCode code, String message) {
        super(HttpStatus.UNAUTHORIZED, code, message);
    }
}
