package com.mopl.domain.review.exception;

import com.mopl.global.exception.DomainErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReviewErrorCode implements DomainErrorCode {

    NOT_REVIEW_OWNER("R001", "사용자가 소유한 리뷰가 아닙니다.", HttpStatus.FORBIDDEN)
    ;

    private final String errorCode;
    private final String message;
    private final HttpStatus httpStatus;
}
