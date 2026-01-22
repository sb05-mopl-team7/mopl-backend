package com.mopl.domain.conversation.dto.response;

import com.mopl.domain.conversation.entity.Conversation;
import com.mopl.domain.conversation.entity.DirectMessage;
import com.mopl.domain.conversation.entity.ReadStatus;
import com.mopl.domain.user.entity.User;

public record ConversationQueryResult(
        Conversation conversation,
        User targetUser,
        DirectMessage lastMessage,
        ReadStatus myReadStatus
) {
}
