package com.mopl.domain.conversation.dto.response;

import com.mopl.domain.user.dto.response.UserSummaryDto;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

public record ConversationDto(
        @JsonSerialize(using = ToStringSerializer.class)
        Long id,
        UserSummaryDto with,
        DirectMessageDto lastestMessage,
        boolean hasUnread
) {
}
