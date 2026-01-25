package com.mopl.domain.watching.exception;

import com.mopl.global.exception.DomainErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum WatchingErrorCode implements DomainErrorCode {

    // 특정 사용자의 시청 세션 단건 조회 예외 코드
    INVALID_WATCHING_REQUEST("W001", "잘못된 시청 세션 조회 요청입니다.", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND("W002", "존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND),
    CONTENT_NOT_FOUND("W003", "콘텐츠 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 특정 콘텐츠의 시청 세션 목록 조회 예외 코드
    INVALID_CURSOR("W004", "유효하지 않은 커서 값입니다.", HttpStatus.BAD_REQUEST),
    INVALID_PAGINATION_LIMIT("W005", "페이지 요청 개수가 유효하지 않습니다.", HttpStatus.BAD_REQUEST),

    // 세션 강제 조작 방지 등 추후 확장용
    WATCHING_SESSION_EXPIRED("W006", "시청 세션이 만료되었습니다.", HttpStatus.GONE) // 410 or 404
    ;

    private final String errorCode;
    private final String message;
    private final HttpStatus httpStatus;
}