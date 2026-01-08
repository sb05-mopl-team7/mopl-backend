package com.mopl.domain.conversation.dto.request;

import jakarta.validation.constraints.NotNull;

public record ConversationCreateRequest(
        @NotNull(message = "대화 상대는 필수값입니다.")
        Long withUserId
) {
}
