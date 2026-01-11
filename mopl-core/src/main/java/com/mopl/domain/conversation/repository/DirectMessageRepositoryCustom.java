package com.mopl.domain.conversation.repository;

import com.mopl.domain.conversation.entity.DirectMessage;

import java.time.LocalDateTime;
import java.util.List;

public interface DirectMessageRepositoryCustom {
    List<DirectMessage> findAllMessagesByCursor(Long conversationId, LocalDateTime cursor, Long idAfter, int limit);
}
