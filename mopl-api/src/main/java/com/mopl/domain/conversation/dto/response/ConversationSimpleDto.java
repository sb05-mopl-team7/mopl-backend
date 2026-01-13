package com.mopl.domain.conversation.dto.response;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

public record ConversationSimpleDto(
        @JsonSerialize(using = ToStringSerializer.class)
        Long id // conversationId
) {
}
