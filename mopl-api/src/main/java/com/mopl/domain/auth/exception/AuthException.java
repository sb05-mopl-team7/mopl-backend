package com.mopl.domain.auth.exception;

import com.mopl.global.exception.DomainException;
import lombok.Getter;

@Getter
public class AuthException extends DomainException {

    public AuthException(AuthErrorCode errorCode) {
        super(errorCode);
    }
}
