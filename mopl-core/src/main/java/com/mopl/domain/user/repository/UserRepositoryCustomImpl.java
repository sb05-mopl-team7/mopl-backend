package com.mopl.domain.user.repository;

import com.mopl.domain.user.entity.QUser;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.enums.Role;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<User> cursorFindAll(
            String keywordLike,
            Role roleEqual,
            Boolean isLocked,
            LocalDateTime cursorCreatedAt,
            Long idAfter,
            Pageable pageable) {

        QUser user = QUser.user;

        //필터 조건
        BooleanBuilder filterCondition = buildFilterConditions(user, keywordLike, roleEqual, isLocked);

        // 커서 조건
        BooleanExpression cursorCondition = buildCursorCondition(user, cursorCreatedAt, idAfter, pageable);

        //쿼리 실행
        JPAQuery<User> query = jpaQueryFactory
                .selectFrom(user)
                .where(filterCondition, cursorCondition);

        //정렬 적용
        applySorting(query, user,pageable);
        return query.limit(pageable.getPageSize()).fetch();
    }
    private BooleanBuilder buildFilterConditions(
            QUser user,
            String keywordLike,
            Role roleEqual,
            Boolean isLocked
    ){
        BooleanBuilder builder = new BooleanBuilder();

        //emailLike 조건
        if(keywordLike != null && !keywordLike.isBlank()){
            builder.and(user.email.contains(keywordLike));
        }

        //roleEqual 조건
        if(roleEqual != null){
            builder.and(user.role.eq(roleEqual));
        }

        //isLocked 조건
        if(isLocked != null){
            builder.and(user.locked.eq(isLocked));
        }
        return builder;
    }
    private BooleanExpression buildCursorCondition(
            QUser user,
            LocalDateTime cursorCreatedAt,
            Long idAfter,
            Pageable pageable
    ){
        if(cursorCreatedAt == null || idAfter == null){
            return null;
        }

        // Pageable에서 정렬 방향 추출
        Sort.Direction direction = extractCreatedAtDirection(pageable);

        // 정렬 방향에 따라 비교 연산자 변경
        if (direction == Sort.Direction.DESC) {
            // DESCENDING: 작은 값
            return user.createdAt.lt(cursorCreatedAt)
                    .or(user.createdAt.eq(cursorCreatedAt)
                            .and(user.id.lt(idAfter)));
        } else {
            // ASCENDING: 큰 값
            return user.createdAt.gt(cursorCreatedAt)
                    .or(user.createdAt.eq(cursorCreatedAt)
                            .and(user.id.gt(idAfter)));
        }
    }
    // Pageable에서 정렬 방향 추출
    private Sort.Direction extractCreatedAtDirection(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (order.getProperty().equals("createdAt")) {
                return order.getDirection();
            }
        }
        return Sort.Direction.DESC;
    }
    // 정렬 적용
    private void applySorting(JPAQuery<User> query, QUser user, Pageable pageable) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        for (Sort.Order order : pageable.getSort()) {
            String property = order.getProperty();
            boolean isAsc = order.isAscending();

            if ("name".equals(property)) {
                orderSpecifiers.add(isAsc ? user.name.asc() : user.name.desc());
            } else if ("email".equals(property)) {
                orderSpecifiers.add(isAsc ? user.email.asc() : user.email.desc());
            } else if ("createdAt".equals(property)) {
                orderSpecifiers.add(isAsc ? user.createdAt.asc() : user.createdAt.desc());
            } else if ("locked".equals(property)) {
                orderSpecifiers.add(isAsc ? user.locked.asc() : user.locked.desc());
            } else if ("role".equals(property)) {
                orderSpecifiers.add(isAsc ? user.role.asc() : user.role.desc());
            } else if ("id".equals(property)) {
                orderSpecifiers.add(isAsc ? user.id.asc() : user.id.desc());
            }
        }

        if (!orderSpecifiers.isEmpty()) {
            query.orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]));
        }
    }

}
