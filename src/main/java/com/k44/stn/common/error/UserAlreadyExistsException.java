package com.k44.stn.common.error;

import org.springframework.http.HttpStatus;

public final class UserAlreadyExistsException extends ApiException {
    public UserAlreadyExistsException(ErrorCode code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
