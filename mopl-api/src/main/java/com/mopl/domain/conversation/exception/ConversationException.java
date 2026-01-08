package com.mopl.domain.conversation.exception;

import lombok.Getter;

@Getter
public class ConversationException extends RuntimeException {

    private final ConversationErrorCode errorCode;

    public ConversationException(ConversationErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
