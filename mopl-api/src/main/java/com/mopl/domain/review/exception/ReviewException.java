package com.mopl.domain.review.exception;

import com.mopl.global.exception.DomainException;
import lombok.Getter;

@Getter
public class ReviewException extends DomainException {
    public ReviewException(ReviewErrorCode errorCode) {
        super(errorCode);
    }
}
