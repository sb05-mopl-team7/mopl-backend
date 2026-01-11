package com.mopl.domain.conversation.repository;

import com.mopl.domain.conversation.entity.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long>, DirectMessageRepositoryCustom {

    // createdAt이 같을 수 있으므로 id로 2차 정렬
    Optional<DirectMessage> findTopByConversationIdOrderByCreatedAtDescIdDesc(Long conversationId);

    Optional<DirectMessage> findByIdAndConversationId(Long directMessageId, Long conversationId);
}
