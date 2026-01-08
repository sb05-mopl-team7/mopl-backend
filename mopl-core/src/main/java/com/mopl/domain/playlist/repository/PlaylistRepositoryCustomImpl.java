package com.mopl.domain.playlist.repository;

import com.mopl.domain.playlist.entity.Playlist;
import com.mopl.domain.playlist.entity.QPlaylist;
import com.mopl.domain.playlist.entity.QPlaylistSubscribe;
import com.mopl.global.enums.SortDirection;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PlaylistRepositoryCustomImpl implements PlaylistRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QPlaylist playlist = QPlaylist.playlist;
    private final QPlaylistSubscribe subscribe = QPlaylistSubscribe.playlistSubscribe;

    @Override
    public List<Playlist> cursorFindAll(
            String keywordLike,
            Long ownerIdEqual,
            Long subscriberIdEqual,
            LocalDateTime cursorUpdatedAt,
            Long cursorSubscriberCount,
            Long idAfter,
            int limitPlusOne,
            String sortBy,
            SortDirection sortDirection
    ) {
        BooleanBuilder where = new BooleanBuilder();

        // (title/description)
        if (keywordLike != null && !keywordLike.isBlank()) {
            where.and(
                    playlist.title.contains(keywordLike)
                            .or(playlist.description.contains(keywordLike))
            );
        }

        // ownerIdEqual
        if (ownerIdEqual != null) {
            where.and(playlist.userId.eq(ownerIdEqual));
        }

        // (구독한 목록 + 본인 소유도 포함)
        if (subscriberIdEqual != null) {
            where.and(
                    playlist.userId.eq(subscriberIdEqual)
                            .or(
                                    JPAExpressions.selectOne()
                                            .from(subscribe)
                                            .where(
                                                    subscribe.userId.eq(subscriberIdEqual)
                                                            .and(subscribe.playlistId.eq(playlist.id))
                                            )
                                            .exists()
                            )
            );
        }

        boolean isDesc = (sortDirection == null) || sortDirection == SortDirection.DESCENDING;

        // 커서 조건 (cursor + idAfter)
        if ("updatedAt".equalsIgnoreCase(sortBy)) {
            if (cursorUpdatedAt != null && idAfter != null) {
                if (isDesc) {
                    where.and(
                            playlist.updatedAt.lt(cursorUpdatedAt)
                                    .or(playlist.updatedAt.eq(cursorUpdatedAt).and(playlist.id.lt(idAfter)))
                    );
                } else {
                    where.and(
                            playlist.updatedAt.gt(cursorUpdatedAt)
                                    .or(playlist.updatedAt.eq(cursorUpdatedAt).and(playlist.id.gt(idAfter)))
                    );
                }
            }
        } else { // subscriberCount 정렬
            if (cursorSubscriberCount != null && idAfter != null) {
                if (isDesc) {
                    where.and(
                            playlist.subscriberCount.lt(cursorSubscriberCount)
                                    .or(playlist.subscriberCount.eq(cursorSubscriberCount).and(playlist.id.lt(idAfter)))
                    );
                } else {
                    where.and(
                            playlist.subscriberCount.gt(cursorSubscriberCount)
                                    .or(playlist.subscriberCount.eq(cursorSubscriberCount).and(playlist.id.gt(idAfter)))
                    );
                }
            }
        }

        Order order = isDesc ? Order.DESC : Order.ASC;
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        if ("updatedAt".equalsIgnoreCase(sortBy)) {
            orderSpecifiers.add(new OrderSpecifier<>(order, playlist.updatedAt));
        } else {
            orderSpecifiers.add(new OrderSpecifier<>(order, playlist.subscriberCount));
        }
        orderSpecifiers.add(new OrderSpecifier<>(order, playlist.id)); // tie-breaker

        return queryFactory.selectFrom(playlist)
                .where(where)
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .limit(limitPlusOne)
                .fetch();
    }
}