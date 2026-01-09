package com.mopl.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 예상치 못한 시스템 예외 (500) - ErrorCode.INTERNAL_SERVER_ERROR와 연결 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<@NonNull ProblemDetail> handleException(Exception e) {
        log.error("예상치 못한 오류 발생: {}", e.getMessage(), e);
        return createErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    /** 비즈니스 로직 예외 (Custom Exception) - MoplException 안에 있는 ErrorCode와 연결 */
    @ExceptionHandler(MoplException.class)
    public ResponseEntity<@NonNull ProblemDetail> handleMoplException(MoplException e) {
        log.error("MoplException 발생: {} - {}", e.getErrorCode().getStatus(), e.getMessage(), e);
        return createErrorResponse(e.getErrorCode(), e.getMessage());
    }

    /** 타입 불일치 예외 (400) - ErrorCode.INVALID_INPUT_VALUE와 연결 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<@NonNull ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException e) {

        String detail = String.format("파라미터 '%s'의 값이 유효하지 않습니다. (입력값: %s)", e.getName(), e.getValue());

        log.error("타입 불일치 오류 발생: {}", detail, e);

        return createErrorResponse(ErrorCode.INVALID_INPUT_VALUE, detail);
    }

    /** 권한 부족 예외 (403) - ErrorCode.INSUFFICIENT_PERMISSIONS과 연결 */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<@NonNull ProblemDetail> handleAuthorizationDeniedException(AuthorizationDeniedException e) {

        log.error("권한 부족: {}", ErrorCode.INSUFFICIENT_PERMISSIONS.getMessage(), e);

        return createErrorResponse(ErrorCode.INSUFFICIENT_PERMISSIONS, ErrorCode.INSUFFICIENT_PERMISSIONS.getMessage());
    }

    /** 공통 응답 생성 메서드 */
    private ResponseEntity<@NonNull ProblemDetail> createErrorResponse(ErrorCode errorCode, String detail) {

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(errorCode.getStatus()), detail);

        pd.setTitle(errorCode.name());
        pd.setProperty("status", errorCode.getStatus());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(pd);
    }
}
