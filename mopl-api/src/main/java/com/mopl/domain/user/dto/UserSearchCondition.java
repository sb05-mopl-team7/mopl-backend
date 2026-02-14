package com.mopl.domain.user.dto;

import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.enums.Role;
import com.mopl.global.enums.SortBy;
import com.mopl.global.enums.SortDirection;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public record UserSearchCondition(
        String emailLike,
        Role roleEqual,
        Boolean isLocked,
        String cursor,
        String idAfter,
        @NotNull(message = "limit 값은 필수입니다.")
        @Min(value = 1, message = "조회 개수는 1 이상이어야 합니다.")
        @Max(value = 50, message = "조회 개수는 50을 초과할 수 없습니다.")
        Integer limit,
        SortDirection sortDirection,
        SortBy sortBy
) {
    public UserSearchCondition {
        if (sortDirection == null) {
            sortDirection = SortDirection.ASCENDING;
        }
        if (sortBy == null) {
            sortBy = SortBy.name;
        }
        boolean hasCursor = cursor != null && !cursor.isBlank();
        boolean hasIdAfter = idAfter != null && !idAfter.isBlank();
        if (hasCursor != hasIdAfter) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }
    }

    /**
     * User 엔티티로부터 다음 커서 값 생성
     * @param user 페이지의 마지막 User
     * @return 다음 요청에 사용할 커서 문자열
     */
    public String formatCursor(User user) {
        return switch (sortBy) {
            case name -> user.getName();
            case email -> user.getEmail();
            case createdAt -> user.getCreatedAt()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            case role -> user.getRole().name();
            case isLocked -> String.valueOf(user.getLocked());
        };
    }

    /**
     * 커서 페이징 시작점 정보
     */
    public record StartId(String sortByProperty, Object cursorValue, Long idAfter) {
    }
    /**
     * 커서가 존재하는지 확인
     */
    public boolean hasCursor() {
        return cursor != null && !cursor.isBlank();
    }

    /**
     * 커서 기반 페이징을 위한 시작점 파싱
     *
     * @return StartId (sortBy 컬럼명, 커서 값, ID)
     */
    public StartId parseStartId() {
        if (!hasCursor()) {
            return new StartId(sortBy.property(), null, null);
        }

        Object cursorValue = parseCursorValue();
        Long id = parseLong(idAfter);
        return new StartId(sortBy.property(), cursorValue, id);
    }
    /**
     * sortBy에 따라 cursor 문자열을 적절한 타입으로 파싱
     */
    private Object parseCursorValue() {
        try {
            return switch (sortBy) {
                case name, email -> cursor;
                case createdAt -> parseCreatedAt(cursor);
                case role -> Role.valueOf(cursor.toUpperCase());
                case isLocked -> Boolean.parseBoolean(cursor);
            };
        } catch (DateTimeParseException | IllegalArgumentException e) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }
    }

    /**
     * ISO 형식의 날짜 문자열을 LocalDateTime으로 파싱
     */
    private LocalDateTime parseCreatedAt(String cursor) {
        String normalized = cursor.trim().replace(" ", "T");
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }
    }

    /**
     * idAfter 문자열을 Long으로 파싱
     */
    private Long parseLong(String idAfter) {
        try {
            return Long.parseLong(idAfter.trim());
        } catch (NumberFormatException e) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }
    }
}
