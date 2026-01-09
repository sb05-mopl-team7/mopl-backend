package com.mopl.global.exception;

import lombok.Getter;


@Getter
public class DomainException extends RuntimeException {

    private final DomainErrorCode errorCode;

    // 도메인 예외 처리는 ErrorCode만 사용합니다.
    public DomainException(DomainErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // 커스텀 메시지를 받는 생성자(protected DomainException(String msg, ...)) 만들지 마세요.
}