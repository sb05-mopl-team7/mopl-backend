package com.mopl.domain.follow.exception;

import com.mopl.global.exception.DomainException;
import lombok.Getter;

@Getter
public class FollowException extends DomainException {

    public FollowException(FollowErrorCode errorCode) {
        super(errorCode);
    }
}