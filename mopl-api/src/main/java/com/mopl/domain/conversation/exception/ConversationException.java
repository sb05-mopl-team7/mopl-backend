package com.mopl.domain.conversation.exception;

import com.mopl.global.exception.DomainException;
import lombok.Getter;

@Getter
public class ConversationException extends DomainException {
    public ConversationException(ConversationErrorCode errorCode) {
        super(errorCode);
    }
}
