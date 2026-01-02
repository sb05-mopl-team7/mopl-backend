package com.mopl.domain.content.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ContentErrorCode {

    INVALID_THUMBNAIL("C001", "썸네일 이미지가 비어있거나 올바르지 않습니다.", HttpStatus.UNAUTHORIZED)
    ;

    private final String errorCode;
    private final String message;
    private final HttpStatus httpStatus;
}
