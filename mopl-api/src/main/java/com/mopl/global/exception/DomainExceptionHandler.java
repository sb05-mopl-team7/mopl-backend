package com.mopl.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class DomainExceptionHandler {

    /**
     * 도메인별 공통 비즈니스 예외를 처리합니다.
     *
     * - 반드시 도메인별 ErrorCode(DomainErrorCode 구현체)를 통해서만 예외를 던져야 합니다.
     * - 일관된 에러 메시지 관리를 위해 별도의 커스텀 메시지 사용은 지양합니다.
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomainException(DomainException e) {

        DomainErrorCode errorCode = e.getErrorCode();

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(errorCode.getHttpStatus(), errorCode.getMessage());

        pd.setTitle(errorCode.name());
        pd.setProperty("code", errorCode.getErrorCode());
        pd.setDetail(errorCode.getMessage());
        pd.setStatus(errorCode.getHttpStatus().value());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(pd);
    }
}
