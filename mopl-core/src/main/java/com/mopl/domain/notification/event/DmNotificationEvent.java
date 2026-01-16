package com.mopl.domain.notification.event;

import com.mopl.domain.conversation.dto.response.DirectMessageDto;

public record DmNotificationEvent(
        Long receiverId,
        DirectMessageDto directMessageDto
) {
}
