package com.mopl.domain.playlist.exception;

import com.mopl.global.exception.DomainErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PlaylistErrorCode implements DomainErrorCode {

    PLAYLIST_NOT_FOUND("P001", "플레이리스트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PLAYLIST_FORBIDDEN("P002", "플레이리스트 권한이 없습니다.", HttpStatus.FORBIDDEN),
    PLAYLIST_INVALID_REQUEST("P003", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    CONTENT_NOT_FOUND("P004", "콘텐츠를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ;

    private final String errorCode;
    private final String message;
    private final HttpStatus httpStatus;
}