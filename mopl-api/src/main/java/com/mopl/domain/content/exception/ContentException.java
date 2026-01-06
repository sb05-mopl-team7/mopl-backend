package com.mopl.domain.content.exception;

import lombok.Getter;

@Getter
public class ContentException extends RuntimeException {

    private final ContentErrorCode errorCode;

    public ContentException(ContentErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
