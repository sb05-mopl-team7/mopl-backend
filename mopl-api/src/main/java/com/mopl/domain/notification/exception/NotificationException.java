package com.mopl.domain.notification.exception;

import com.mopl.global.exception.DomainException;
import lombok.Getter;

@Getter
public class NotificationException extends DomainException {

    public NotificationException(NotificationErrorCode errorCode) {
        super(errorCode);
    }
}
