package com.mopl.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class ErrorResponse {
    private final String exceptionName;
    private final String message;
    private final Map<String, Object> details;

    public ErrorResponse(Exception exception, ErrorCode errorCode) {
        this(errorCode.name(), errorCode.getMessage(),new HashMap<>());
    }

    public ErrorResponse(MoplException exception) {
        this(exception.getErrorCode().name(), exception.getMessage(), exception.getDetails());
    }
}
