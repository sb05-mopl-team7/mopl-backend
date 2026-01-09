package com.mopl.domain.conversation.exception;

import com.mopl.global.exception.DomainErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ConversationErrorCode implements DomainErrorCode {
    CONVERSATION_NOT_FOUND("CV001", "대화방을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SELF_CONVERSATION_NOT_ALLOWED("CV002", "자기 자신과는 대화할 수 없습니다.", HttpStatus.BAD_REQUEST)
    ;

    private final String errorCode;
    private final String message;
    private final HttpStatus httpStatus;
}
