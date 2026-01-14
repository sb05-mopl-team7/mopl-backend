package com.mopl.domain.watching.exception;

import com.mopl.global.exception.DomainErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum WatchingErrorCode implements DomainErrorCode {

    // 특정 사용자의 시청 세션 단건 조회 예외 코드
    INVALID_WATCHING_REQUEST("W001", "잘못된 시청 세션 요청입니다.", HttpStatus.BAD_REQUEST), // 400
    WATCHING_SESSION_NOT_FOUND("W002", "시청 세션이 존재하지 않거나 이미 종료되었습니다.", HttpStatus.NOT_FOUND), // 404
    USER_NOT_FOUND("W003", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND), // 404
    CONTENT_NOT_FOUND("W004", "시청 중인 콘텐츠 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND), // 404

    // 특정 콘텐츠의 시청 세션 목록 조회 예외 코드
    INVALID_CURSOR("W005", "유효하지 않은 커서 값입니다.", HttpStatus.BAD_REQUEST), // 400
    INVALID_PAGINATION_LIMIT("W006", "페이지 요청 개수는 1개 이상 100개 이하이어야 합니다.", HttpStatus.BAD_REQUEST) // 400
    ;

    private final String errorCode;
    private final String message;
    private final HttpStatus httpStatus;
}