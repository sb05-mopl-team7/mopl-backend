package com.mopl.domain.watching.exception;

import com.mopl.global.exception.DomainException;
import lombok.Getter;

@Getter
public class WatchingException extends DomainException {

    public WatchingException(WatchingErrorCode errorCode) {
        super(errorCode);
    }
}