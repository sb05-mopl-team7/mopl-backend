package com.mopl.domain.user.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@Slf4j
@RestControllerAdvice
public class UserExceptionHandler {
    @ExceptionHandler(UserException.class)
    public ResponseEntity<ProblemDetail> handleAuthException(UserException e) {

        UserErrorCode errorCode = e.getErrorCode();
        log.error("AuthException 발생: {}", e.getMessage(), e);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(errorCode.getHttpStatus(), e.getMessage());
        pd.setTitle(errorCode.name());
        pd.setProperty("code", errorCode.getErrorCode());
        pd.setType(URI.create("https://mopl.com/problems/" + errorCode.name().toLowerCase()));
        pd.setDetail(e.getMessage());
        pd.setStatus(errorCode.getHttpStatus().value());

        return new ResponseEntity<>(pd, errorCode.getHttpStatus());
    }
}
