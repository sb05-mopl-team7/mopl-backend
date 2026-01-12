package com.mopl.domain.conversation.repository;

import com.mopl.domain.conversation.dto.ConversationQueryResult;
import com.mopl.domain.conversation.entity.QDirectMessage;
import com.mopl.domain.conversation.entity.QReadStatus;
import com.mopl.domain.user.entity.QUser;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.mopl.domain.conversation.entity.QConversation.conversation;

@Repository
@RequiredArgsConstructor
public class ConversationRepositoryCustomImpl implements ConversationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 내가 참여한 대화방 목록 조회
     * - 정렬: 최신 메시지 시간 기준, 빈 대화방이면 대화방 생성일 사용
     * - 조회: 상대방 정보, 최신 메시지, 나의 읽음 상태를 동시 조회
     */
    @Override
    public List<ConversationQueryResult> findMyConversations(Long userId, String keyword, LocalDateTime cursor, Long idAfter, int limit) {
        QReadStatus myStatus = new QReadStatus("myStatus");
        QReadStatus targetStatus = new QReadStatus("targetStatus");
        QUser targetUser = new QUser("targetUser");
        QDirectMessage message = new QDirectMessage("message");
        QDirectMessage subMessage = new QDirectMessage("subMessage");

        // 각 대화방의 최신 메시지 id
        JPQLQuery<Long> lastMessageIdSubquery = JPAExpressions
                .select(subMessage.id.max())
                .from(subMessage)
                .where(subMessage.conversation.eq(conversation));

        // 메시지가 없는 방을 고려해 DM 시간이 null 이면 방 생성 시간을 사용
        DateTimeExpression<LocalDateTime> sortDateTime = message.createdAt.coalesce(conversation.createdAt);

        return queryFactory
                .select(Projections.constructor(ConversationQueryResult.class,
                        conversation,
                        targetUser,
                        message,
                        myStatus
                )).from(conversation)
                // 로그인한 유저가 참여한 방
                .join(myStatus).on(myStatus.conversation.eq(conversation).and(myStatus.user.id.eq(userId)))
                // 대화 상대 정보
                .join(targetStatus).on(targetStatus.conversation.eq(conversation).and(targetStatus.user.id.ne(userId)))
                .join(targetUser).on(targetStatus.user.eq(targetUser))
                // 최신 메시지 (메시지 없는 방 고려해 left join)
                .leftJoin(message).on(message.id.eq(lastMessageIdSubquery))
                .where(
                        keywordSearch(keyword, targetUser), // 검색어 조건
                        cursorCondition(cursor, idAfter, sortDateTime) // 커서 조건
                ).orderBy(sortDateTime.desc(), conversation.id.desc())
                .limit(limit)
                .fetch();
    }

    /**
     * 현재 미사용 메서드지만 추후 필요시 사용할 목적으로 작성
     * 특정 사용자와의 대화방 상세 정보 조회
     * - 조회: 상대방 정보, 최신 메시지, 나의 읽음 상태를 동시 조회
     */
    @Override
    public Optional<ConversationQueryResult> findConversationQueryResult(Long myId, Long withUserId) {
        QUser targetUser = new QUser("targetUser");
        QDirectMessage lastMessage = new QDirectMessage("lastMessage");
        QReadStatus myStatus = new QReadStatus("myStatus");
        QReadStatus targetStatus = new QReadStatus("targetStatus");
        QDirectMessage subMessage = new QDirectMessage("subMessage");

        // 각 대화방 최신 메시지 id 서브쿼리
        JPQLQuery<Long> lastMessageIdSubquery = JPAExpressions
                .select(subMessage.id.max())
                .from(subMessage)
                .where(subMessage.conversation.eq(conversation));

        return Optional.ofNullable(queryFactory.select(Projections.constructor(ConversationQueryResult.class,
                        conversation,
                        targetUser,
                        lastMessage,
                        myStatus
                )).from(conversation)
                // 로그인한 유저가 참여한 방
                .join(myStatus).on(myStatus.conversation.eq(conversation).and(myStatus.user.id.eq(myId)))
                // 대화 상대 정보
                .join(targetStatus).on(targetStatus.conversation.eq(conversation).and(targetStatus.user.id.eq(withUserId)))
                .join(targetUser).on(targetStatus.user.eq(targetUser))
                // 최신 메시지 (메시지 없는 방 고려해 left join)
                .leftJoin(lastMessage).on(lastMessage.id.eq(lastMessageIdSubquery))
                .fetchOne());
    }

    // 검색 조건 - 상대 이름 or 메시지 내용
    private BooleanExpression keywordSearch(String keyword, QUser targetUser) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        BooleanExpression nameSearchCond = targetUser.name.contains(keyword);

        QDirectMessage searchMessage = new QDirectMessage("searchMessage");
        BooleanExpression messageSearchCond = JPAExpressions
                .selectOne()
                .from(searchMessage)
                .where(searchMessage.conversation.eq(conversation)
                        .and(searchMessage.content.contains(keyword)))
                .exists();

        return nameSearchCond.or(messageSearchCond);
    }

    // 커서 조건 - 1: 메시지 생성일 (없다면 대화방 생성일), 2: 대화방 id
    private Predicate cursorCondition(LocalDateTime cursor, Long idAfter, DateTimeExpression<LocalDateTime> sortDateTime) {
        if (cursor == null || idAfter == null) {
            return null;
        }

        return sortDateTime.lt(cursor)
                .or(sortDateTime.eq(cursor).and(conversation.id.lt(idAfter)));
    }
}
