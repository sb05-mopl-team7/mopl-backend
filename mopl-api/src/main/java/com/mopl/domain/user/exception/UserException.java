package com.mopl.domain.user.exception;

import com.mopl.global.exception.DomainException;
import lombok.Getter;

@Getter
public class UserException extends DomainException {

    public UserException(UserErrorCode errorCode) {
        super(errorCode);
    }
}
