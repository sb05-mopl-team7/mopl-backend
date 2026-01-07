package com.mopl.domain.content.repository;


import com.mopl.domain.content.dto.ContentQueryParams;
import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.entity.QContent;
import com.mopl.domain.content.enums.ContentType;
import com.mopl.global.enums.SortDirection;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ContentRepositoryCustomImpl implements ContentRepositoryCustom{

    private final JPAQueryFactory queryFactory;
    QContent content = QContent.content;

    @Override
    public List<Content> list(ContentQueryParams params) {
        BooleanBuilder where = new BooleanBuilder();

        if(params.typeEqual() != null && !params.typeEqual().isEmpty()) {
            where.and(content.contentType.eq(ContentType.valueOf(params.typeEqual())));
        }

        if(params.keywordLike() != null && !params.keywordLike().isBlank()) {
            where.and(content.title.contains(params.keywordLike())
                    .or(content.description.contains(params.keywordLike())));
        }

        if(params.tagsIn() != null && !params.tagsIn().isEmpty()) {
            where.and(content.contentTags.any().tag.tag.in(params.tagsIn()));
        }

        OrderSpecifier<?>[] orderSpecifier = makeOrderSpecifier(params.sortDirection(), params.sortBy());

        return queryFactory.selectFrom(content)
                .distinct()
                .where(where)
                .orderBy(orderSpecifier)
                .limit(params.limit() + 1)
                .fetch();
    }

    private OrderSpecifier<?>[] makeOrderSpecifier(SortDirection direction, String sortBy) {
        Order order = "DESCENDING".equalsIgnoreCase(direction.toString()) ? Order.DESC : Order.ASC;

        // 정렬 조건
        List<OrderSpecifier<?>> specifiers = new ArrayList<>();

        switch (sortBy) {
            case "rate" -> specifiers.add(new OrderSpecifier<>(order, content.averageRating));
            case "watcherCount" -> {
                specifiers.add(new OrderSpecifier<>(order, content.reviewCount));
                specifiers.add(new OrderSpecifier<>(order, content.averageRating)); // 리뷰 수 같으면 평점 순으로
            }
            default -> specifiers.add(new OrderSpecifier<>(order, content.createdAt));
        }

        // 보조 정렬 조건 추가 (항상 마지막에 ID 역순) - 커서 기반 페이징용
        specifiers.add(content.id.desc());

        return specifiers.toArray(new OrderSpecifier[0]);
    }
}
