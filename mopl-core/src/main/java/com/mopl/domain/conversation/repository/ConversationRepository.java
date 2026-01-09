package com.mopl.domain.conversation.repository;

import com.mopl.domain.conversation.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long>, ConversationRepositoryCustom {

    @Query("""
            SELECT c FROM Conversation c
            JOIN ReadStatus rs1 ON c.id = rs1.conversation.id
            JOIN ReadStatus rs2 ON c.id = rs2.conversation.id
            WHERE rs1.user.id = :myId AND rs2.user.id = :targetId
            """)
    Optional<Conversation> findByParticipants(@Param("myId") Long myId, @Param("targetId") Long targetId);
}
