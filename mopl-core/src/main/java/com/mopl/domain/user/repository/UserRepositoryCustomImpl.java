package com.mopl.domain.user.repository;

import com.mopl.domain.user.entity.QUser;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.enums.Role;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.*;
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
            String sortByProperty,
            Object cursorValue,
            Long idAfter,
            Pageable pageable) {

        QUser user = QUser.user;

        //필터 조건
        BooleanBuilder filterCondition = buildFilterConditions(user, keywordLike, roleEqual, isLocked);

        // 커서 조건
        BooleanExpression cursorCondition = buildCursorCondition(user, sortByProperty, cursorValue,idAfter, pageable);

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
            String sortByProperty,
            Object cursorValue,
            Long idAfter,
            Pageable pageable
    ) {
        if (cursorValue == null || idAfter == null) {
            return null;
        }

        // sortByProperty의 정렬 방향 추출
        Sort.Direction direction = extractSortDirection(pageable, sortByProperty);

        // sortByProperty에 따라 비교 로직 분기
        switch (sortByProperty) {
            case "name":
                return buildStringCursorCondition(
                        user.name, (String) cursorValue, user.id, idAfter, direction
                );

            case "email":
                return buildStringCursorCondition(
                        user.email, (String) cursorValue, user.id, idAfter, direction
                );

            case "createdAt":
                return buildDateTimeCursorCondition(
                        user.createdAt, (LocalDateTime) cursorValue, user.id, idAfter, direction
                );

            case "role":
                return buildRoleCursorCondition(
                        user.role, (Role) cursorValue, user.id, idAfter, direction
                );

            case "locked":
                return buildBooleanCursorCondition(
                        user.locked, (Boolean) cursorValue, user.id, idAfter, direction
                );

            default:
                return null;
        }
    }
    private <T extends Comparable<? super T>> BooleanExpression buildComparableCursorCondition(
            ComparablePath<T> field,
            T cursorValue,
            NumberPath<Long> idField,
            Long idAfter,
            Sort.Direction direction
    ) {
        if (direction == Sort.Direction.DESC) {
            return field.lt(cursorValue)
                    .or(field.eq(cursorValue).and(idField.lt(idAfter)));
        } else {
            return field.gt(cursorValue)
                    .or(field.eq(cursorValue).and(idField.gt(idAfter)));
        }
    }
    // ✅ 추가: String 전용
    private BooleanExpression buildStringCursorCondition(
            StringPath field,
            String cursorValue,
            NumberPath<Long> idField,
            Long idAfter,
            Sort.Direction direction
    ) {
        if (direction == Sort.Direction.DESC) {
            return field.lt(cursorValue)
                    .or(field.eq(cursorValue).and(idField.lt(idAfter)));
        } else {
            return field.gt(cursorValue)
                    .or(field.eq(cursorValue).and(idField.gt(idAfter)));
        }
    }

    // ✅ 추가: LocalDateTime 전용
    private BooleanExpression buildDateTimeCursorCondition(
            DateTimePath<LocalDateTime> field,
            LocalDateTime cursorValue,
            NumberPath<Long> idField,
            Long idAfter,
            Sort.Direction direction
    ) {
        if (direction == Sort.Direction.DESC) {
            return field.lt(cursorValue)
                    .or(field.eq(cursorValue).and(idField.lt(idAfter)));
        } else {
            return field.gt(cursorValue)
                    .or(field.eq(cursorValue).and(idField.gt(idAfter)));
        }
    }

    // ✅ 추가: Enum(Role) 전용
    private BooleanExpression buildRoleCursorCondition(
            EnumPath<Role> field,
            Role cursorValue,
            NumberPath<Long> idField,
            Long idAfter,
            Sort.Direction direction
    ) {
        if (direction == Sort.Direction.DESC) {
            return field.lt(cursorValue)
                    .or(field.eq(cursorValue).and(idField.lt(idAfter)));
        } else {
            return field.gt(cursorValue)
                    .or(field.eq(cursorValue).and(idField.gt(idAfter)));
        }
    }

    // ✅ 추가: Boolean 전용
    private BooleanExpression buildBooleanCursorCondition(
            BooleanPath field,
            Boolean cursorValue,
            NumberPath<Long> idField,
            Long idAfter,
            Sort.Direction direction
    ) {
        BooleanExpression baseCondition;

        if (direction == Sort.Direction.DESC) {
            if (cursorValue) {
                baseCondition = field.isFalse();
            } else {
                return idField.lt(idAfter);
            }
        } else {
            if (!cursorValue) {
                baseCondition = field.isTrue();
            } else {
                return idField.gt(idAfter);
            }
        }

        BooleanExpression sameValueCondition = field.eq(cursorValue)
                .and(direction == Sort.Direction.DESC
                        ? idField.lt(idAfter)
                        : idField.gt(idAfter));

        return baseCondition.or(sameValueCondition);
    }


    // Pageable에서 정렬 방향 추출
    private Sort.Direction extractSortDirection(Pageable pageable, String sortByProperty) {
        for (Sort.Order order : pageable.getSort()) {
            if (order.getProperty().equals(sortByProperty)) {
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
