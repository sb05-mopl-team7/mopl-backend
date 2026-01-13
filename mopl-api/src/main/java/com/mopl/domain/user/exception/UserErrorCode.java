package com.mopl.domain.user.exception;

import com.mopl.global.exception.DomainErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements DomainErrorCode {

    USER_NOT_EXIST("U001", "존재하지 않는 사용자 입니다.", HttpStatus.NOT_FOUND),
    PASSWORD_NOT_CORRECT("U002", "아이디 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    DUPLICATE_USER("U003","이미 존재하는 사용자입니다.", HttpStatus.CONFLICT),
//    EMAIL_NOT_EXIST("U004", "존재하지 않는 이메일 입니다.", HttpStatus.NOT_FOUND),
    ;

    private final String errorCode;
    private final String message;
    private final HttpStatus httpStatus;
}
