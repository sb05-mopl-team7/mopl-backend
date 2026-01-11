package com.mopl.domain.auth.exception;

import com.mopl.global.exception.DomainErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements DomainErrorCode {

    REFRESH_TOKEN_INVALID("A001", "유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED),

    ;

    private final String errorCode;
    private final String message;
    private final HttpStatus httpStatus;
}

