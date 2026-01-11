package com.mopl.domain.conversation.exception;

import com.mopl.global.exception.DomainErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ConversationErrorCode implements DomainErrorCode {
    // Conversation
    CONVERSATION_NOT_FOUND("CV001", "대화방을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SELF_CONVERSATION_NOT_ALLOWED("CV002", "자기 자신과는 대화할 수 없습니다.", HttpStatus.BAD_REQUEST),
    CONVERSATION_ID_MISMATCH("CV003", "경로의 대화방 ID와 요청 본문의 ID가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),

    // DM
    MESSAGE_NOT_FOUND("DM001", "메시지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ;

    private final String errorCode;
    private final String message;
    private final HttpStatus httpStatus;
}
