package com.mopl.domain.conversation.repository;

import com.mopl.domain.conversation.entity.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {
}
