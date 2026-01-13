package com.mopl.domain.follow.exception;

import com.mopl.global.exception.DomainErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FollowErrorCode implements DomainErrorCode {

    CANNOT_FOLLOW_SELF("F001", "자기 자신을 팔로우할 수 없습니다.", HttpStatus.BAD_REQUEST), // 400
    ALREADY_FOLLOWING("F002", "이미 팔로우 중인 사용자입니다.", HttpStatus.BAD_REQUEST), // 400
    NOT_YOUR_FOLLOW("F003", "본인의 팔로우만 취소할 수 있습니다.", HttpStatus.FORBIDDEN), //403
    FOLLOW_NOT_FOUND("F004", "팔로우 관계가 존재하지 않습니다.", HttpStatus.NOT_FOUND) // 404
    ;

    private final String errorCode;
    private final String message;
    private final HttpStatus httpStatus;
}