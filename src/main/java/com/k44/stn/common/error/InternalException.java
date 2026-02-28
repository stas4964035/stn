package com.k44.stn.common.error;

import org.springframework.http.HttpStatus;

public final class InternalException extends ApiException {
    public InternalException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, message);
    }
}