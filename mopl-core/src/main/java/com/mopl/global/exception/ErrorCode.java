package com.mopl.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 공통 서버 오류
    INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다.", 500),
    INVALID_REQUEST("잘못된 요청입니다.", 400),

    // 리소스 관련
    NOT_FOUND("요청한 리소스를 찾을 수 없습니다.", 404),
    METHOD_NOT_ALLOWED("허용되지 않은 HTTP 메서드입니다.", 405),

    // 데이터 및 비즈니스 로직 관련
    INVALID_INPUT_VALUE("입력 값이 유효하지 않습니다.", 400),
    DATA_INTEGRITY_VIOLATION("데이터 무결성 제약 조건이 위반되었습니다.", 400),
    DUPLICATE_RESOURCE("이미 존재하는 리소스입니다.", 409),

    // 외부 API 및 시스템
    EXTERNAL_API_ERROR("외부 API 호출 중 오류가 발생했습니다.", 502),

    // 인증
    UNAUTHORIZED("인증되지 않은 사용자입니다.", 401),
    INVALID_TOKEN("유효하지 않은 토큰입니다.", 401),
    EXPIRED_TOKEN("만료된 토큰입니다.", 401),
    TOKEN_NOT_FOUND("토큰을 찾을 수 없습니다.", 401),

    // 인가
    FORBIDDEN("접근 권한이 없습니다.", 403),
    INSUFFICIENT_PERMISSIONS("해당 리소스에 대한 권한이 부족합니다.", 403)
    ;

    private final String message;
    private final int status;
}
