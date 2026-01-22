package com.mopl.domain.conversation.event;

import com.mopl.domain.conversation.dto.response.DirectMessageDto;

public record DmSendEvent(
        Long receiverId,
        DirectMessageDto directMessageDto
) {
}
