package com.mopl.domain.conversation.repository;

import com.mopl.domain.conversation.dto.ConversationQueryResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConversationRepositoryCustom {
    List<ConversationQueryResult> findMyConversations(Long userId, String keyword, LocalDateTime cursor, Long idAfter, int limit);

    Optional<ConversationQueryResult> findConversationQueryResult(Long myId, Long withUserId);
}
