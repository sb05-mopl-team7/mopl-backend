package com.mopl.domain.content.exception;

import com.mopl.global.exception.DomainException;
import lombok.Getter;

@Getter
public class ContentException extends DomainException {
    public ContentException(ContentErrorCode errorCode) {
        super(errorCode);
    }
}
