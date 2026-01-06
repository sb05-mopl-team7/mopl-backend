package com.mopl.global.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class MoplException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public MoplException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }
}
