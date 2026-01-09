package com.mopl.domain.conversation.repository;

import com.querydsl.core.Tuple;

import java.time.LocalDateTime;
import java.util.List;

public interface ConversationRepositoryCustom {
    List<Tuple> findMyConversations(Long userId, String keyword, LocalDateTime cursor, Long idAfter, int limit);
}
