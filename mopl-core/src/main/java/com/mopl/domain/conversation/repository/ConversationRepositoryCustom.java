package com.mopl.domain.conversation.repository;

import com.mopl.domain.conversation.dto.ConversationQueryResult;

import java.time.LocalDateTime;
import java.util.List;

public interface ConversationRepositoryCustom {
    List<ConversationQueryResult> findMyConversations(Long userId, String keyword, LocalDateTime cursor, Long idAfter, int limit);
}
