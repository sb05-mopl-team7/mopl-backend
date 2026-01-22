package com.mopl.domain.directmessage.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DirectMessageSendRequest(
        @NotNull(message = "메시지는 비어있을 수 없습니다.")
        @Size(min = 1, max = 255, message = "메시지 길이는 1-255자 사이여야 합니다.")
        String content
) {
}
