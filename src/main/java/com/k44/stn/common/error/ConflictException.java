package com.k44.stn.common.error;

import org.springframework.http.HttpStatus;

public final class ConflictException extends ApiException {
    public ConflictException(ErrorCode code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}