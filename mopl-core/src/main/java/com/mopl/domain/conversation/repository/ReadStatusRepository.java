package com.mopl.domain.conversation.repository;

import com.mopl.domain.conversation.entity.ReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReadStatusRepository extends JpaRepository<ReadStatus, Long> {

    Optional<ReadStatus> findByConversationIdAndUserId(Long conversationId, Long userId);
}
