package com.mopl.global.exception;

import org.springframework.http.HttpStatus;

public interface DomainErrorCode {

    // 에러코드 - "C001", "U001"
    String getErrorCode();

    // 에러 메시지
    String getMessage();

    // HTTP 상태 코드
    HttpStatus getHttpStatus();

    // Enum의 기본 메서드 (상수명)
    String name();
}
