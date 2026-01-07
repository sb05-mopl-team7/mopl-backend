package com.mopl.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(Exception e) {
        log.error("예상치 못한 오류 발생: {}", e.getMessage(), e);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());

        pd.setTitle("Internal Server Error");
        pd.setType(URI.create("https://mopl.com/problems/internal-server-error"));
        pd.setDetail(e.getMessage());
        pd.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

        return new ResponseEntity<>(pd, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MoplException.class)
    public ResponseEntity<ProblemDetail> handleMynMyException(MoplException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("MoplException 발생: {} - {}", errorCode.getStatus(), e.getMessage(), e);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(errorCode.getStatus()), e.getMessage());
        pd.setTitle(errorCode.name());
        pd.setType(URI.create("https://mopl.com/problems/" + errorCode.name().toLowerCase()));
        pd.setDetail(e.getMessage());
        pd.setStatus(errorCode.getStatus());

        return new ResponseEntity<>(pd, HttpStatusCode.valueOf(errorCode.getStatus()));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        ErrorCode errorCode = ErrorCode.INSUFFICIENT_PERMISSIONS;
        log.error("권한 부족: {} - {}", errorCode.getStatus(), errorCode.getMessage(), e);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatusCode.valueOf(errorCode.getStatus()),
                errorCode.getMessage()
        );
        pd.setTitle(errorCode.name());
        pd.setType(URI.create("https://mopl.com/problems/" + errorCode.name().toLowerCase()));
        pd.setDetail(errorCode.getMessage());
        pd.setStatus(errorCode.getStatus());

        return new ResponseEntity<>(pd, HttpStatusCode.valueOf(errorCode.getStatus()));
    }
    // AccessDeniedException도 같이 처리 (혹시 모를 케이스 대비)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException e) {
        ErrorCode errorCode = ErrorCode.INSUFFICIENT_PERMISSIONS;
        log.error("접근 거부: {} - {}", errorCode.getStatus(), errorCode.getMessage(), e);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatusCode.valueOf(errorCode.getStatus()),
                errorCode.getMessage()
        );
        pd.setTitle(errorCode.name());
        pd.setType(URI.create("https://mopl.com/problems/" + errorCode.name().toLowerCase()));
        pd.setDetail(errorCode.getMessage());
        pd.setStatus(errorCode.getStatus());

        return new ResponseEntity<>(pd, HttpStatusCode.valueOf(errorCode.getStatus()));
    }
}
