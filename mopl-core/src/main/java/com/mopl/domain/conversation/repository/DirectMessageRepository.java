package com.mopl.domain.conversation.repository;

import com.mopl.domain.conversation.entity.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long>, DirectMessageRepositoryCustom {

    // createdAt이 같을 수 있으므로 id로 2차 정렬
    Optional<DirectMessage> findTopByConversationIdOrderByCreatedAtDescIdDesc(Long conversationId);

    Optional<DirectMessage> findByIdAndConversationId(Long directMessageId, Long conversationId);

    @Query("""
        SELECT dm FROM DirectMessage dm
        JOIN FETCH dm.author u
        JOIN ReadStatus rs ON rs.conversation = dm.conversation
        WHERE rs.user.id = :userId
            AND dm.author.id != :userId
            AND (dm.createdAt > :lastTime OR (dm.createdAt = :lastTime AND dm.id > :lastId))
        ORDER BY dm.createdAt ASC, dm.id ASC
    """)
    List<DirectMessage> findMissedMessages(@Param("userId") Long userId, @Param("lastTime") LocalDateTime lastTime, @Param("lastId") Long lastId);
}
