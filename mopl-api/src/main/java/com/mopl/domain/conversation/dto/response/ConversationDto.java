package com.mopl.domain.conversation.dto.response;

import com.mopl.domain.user.dto.response.UserSummaryDto;

public record ConversationDto(
        Long id,
        UserSummaryDto with,
        LastMessage lastestMessage,
        boolean hasUnread
) {
}
