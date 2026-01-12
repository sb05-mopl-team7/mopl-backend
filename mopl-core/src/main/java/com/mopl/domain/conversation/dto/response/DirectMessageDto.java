package com.mopl.domain.conversation.dto.response;

import com.mopl.domain.user.dto.response.UserSummaryDto;

import java.time.LocalDateTime;

public record DirectMessageDto(
        Long id,
        Long conversationId,
        LocalDateTime createdAt,
        UserSummaryDto sender,
        UserSummaryDto receiver,
        String content
) {
}
