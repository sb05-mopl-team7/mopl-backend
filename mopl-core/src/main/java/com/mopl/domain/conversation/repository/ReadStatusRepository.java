package com.mopl.domain.conversation.repository;

import com.mopl.domain.conversation.entity.ReadStatus;
import com.mopl.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReadStatusRepository extends JpaRepository<ReadStatus, Long> {

    Optional<ReadStatus> findByConversationIdAndUserId(Long conversationId, Long userId);

    @Query("""
        SELECT rs.user
        FROM ReadStatus rs
        WHERE rs.conversation.id = :conversationId AND rs.user.id != :userId
    """)
    Optional<User> findPartnerByConversationId(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}
