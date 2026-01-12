package com.mopl.domain.conversation.repository;

import com.mopl.domain.conversation.entity.DirectMessage;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.mopl.domain.conversation.entity.QDirectMessage.directMessage;

@Repository
@RequiredArgsConstructor
public class DirectMessageRepositoryCustomImpl implements DirectMessageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<DirectMessage> findAllMessagesByCursor(Long conversationId, LocalDateTime cursor, Long idAfter, int limit) {

        return queryFactory
                .selectFrom(directMessage)
                .where(
                        directMessage.conversation.id.eq(conversationId),
                        cursorCondition(cursor, idAfter))
                .orderBy(directMessage.createdAt.desc(), directMessage.id.desc()) // 채팅은 최신순 조회로 고정
                .limit(limit)
                .fetch();
    }

    private BooleanExpression cursorCondition(LocalDateTime cursor, Long idAfter) {
        if (cursor == null || idAfter == null) {
            return null;
        }

        return directMessage.createdAt.lt(cursor)
                .or(directMessage.createdAt.eq(cursor).and(directMessage.id.lt(idAfter)));
    }
}
