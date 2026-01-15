package com.mopl.domain.notification.exception;

import com.mopl.global.exception.DomainErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements DomainErrorCode {

    NOTIFICATION_NOT_EXIST("N001", "알림을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    ;

    private final String errorCode;
    private final String message;
    private final HttpStatus httpStatus;
}
