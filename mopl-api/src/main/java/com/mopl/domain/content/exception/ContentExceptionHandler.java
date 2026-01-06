package com.mopl.domain.content.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@Slf4j
@RestControllerAdvice
public class ContentExceptionHandler {

    @ExceptionHandler(ContentException.class)
    public ResponseEntity<ProblemDetail> handleAuthException(ContentException e) {

        ContentErrorCode errorCode = e.getErrorCode();
        log.error("ContentException 발생: {}", e.getMessage(), e);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(errorCode.getHttpStatus(), e.getMessage());
        pd.setTitle(errorCode.name());
        pd.setProperty("code", errorCode.getErrorCode());
        pd.setType(URI.create("https://mopl.com/problems/" + errorCode.name().toLowerCase()));
        pd.setDetail(e.getMessage());
        pd.setStatus(errorCode.getHttpStatus().value());

        return new ResponseEntity<>(pd, errorCode.getHttpStatus());
    }
}
