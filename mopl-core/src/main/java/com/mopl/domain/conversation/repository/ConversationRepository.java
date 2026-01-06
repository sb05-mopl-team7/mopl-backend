package com.mopl.domain.conversation.repository;

import com.mopl.domain.conversation.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

}
