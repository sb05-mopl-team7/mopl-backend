package com.mopl.domain.conversation.dto.response;

import com.mopl.domain.user.dto.response.UserSummaryDto;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;

public record DirectMessageDto(

        @JsonSerialize(using = ToStringSerializer.class)
        Long id,

        @JsonSerialize(using = ToStringSerializer.class)
        Long conversationId,

        LocalDateTime createdAt,
        UserSummaryDto sender,
        UserSummaryDto receiver,
        String content
) {
}
