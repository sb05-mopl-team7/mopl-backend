package com.mopl.domain.playlist.dto.request;

import com.mopl.global.enums.SortDirection;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public record PlaylistSearchCondition(
        String keywordLike,
        Long ownerIdEqual,
        Long subscriberIdEqual,

        // cursor는 sortBy에 따라 의미가 달라서 String으로 받고 내부에서 검증/파싱
        String cursor,

        @Positive(message = "유효한 값을 입력해주세요.")
        Long idAfter,

        @Min(value = 1, message = "조회 개수는 1 이상이어야 합니다.")
        @Max(value = 100, message = "조회 개수는 100을 초과할 수 없습니다.")
        Integer limit,

        SortDirection sortDirection,
        String sortBy
) {
    private static final int DEFAULT_LIMIT = 20;

    public PlaylistSearchCondition {
        // 기본값
        if (limit == null) limit = DEFAULT_LIMIT;
        if (sortDirection == null) sortDirection = SortDirection.DESCENDING;
        if (sortBy == null || sortBy.isBlank()) sortBy = "updatedAt";

        // sortBy 허용값 정규화 + 검증
        sortBy = normalizeSortBy(sortBy);

        // cursor / idAfter는 “쌍”으로만 허용
        boolean hasCursor = cursor != null && !cursor.isBlank();
        boolean hasIdAfter = idAfter != null;
        if (hasCursor != hasIdAfter) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }

        // cursor 형식 검증(바인딩 단계에서 바로 걸러짐)
        if (hasCursor) {
            if ("updatedAt".equalsIgnoreCase(sortBy)) {
                parseDateTimeCursor(cursor);
            } else {
                parseLongCursor(cursor);
            }
        }
    }

    public CursorKey toCursorKey() {
        boolean hasCursor = cursor != null && !cursor.isBlank();
        if (!hasCursor) return new CursorKey(null, null, null);

        if ("updatedAt".equalsIgnoreCase(sortBy)) {
            LocalDateTime updatedAt = parseDateTimeCursor(cursor);
            return new CursorKey(updatedAt, null, idAfter);
        } else {
            Long subscriberCount = parseLongCursor(cursor);
            return new CursorKey(null, subscriberCount, idAfter);
        }
    }

    public String normalizedSortBy() {
        return sortBy;
    }

    public SortDirection normalizedDirection() {
        return sortDirection;
    }

    public int normalizedLimit() {
        return limit;
    }

    public String nextCursorOf(com.mopl.domain.playlist.entity.Playlist last) {
        if (last == null) return null;
        if ("updatedAt".equalsIgnoreCase(sortBy)) {
            return last.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return String.valueOf(last.getSubscriberCount());
    }

    private static String normalizeSortBy(String sortBy) {
        String v = sortBy.trim();
        if ("updatedAt".equalsIgnoreCase(v)) return "updatedAt";
        if ("subscribeCount".equalsIgnoreCase(v) || "subscriberCount".equalsIgnoreCase(v)) return "subscriberCount";
        throw new MoplException(ErrorCode.INVALID_REQUEST);
    }

    private static Long parseLongCursor(String raw) {
        try {
            return Long.parseLong(raw.trim());
        } catch (Exception e) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }
    }

    private static LocalDateTime parseDateTimeCursor(String raw) {
        String normalized = raw.trim().replace(" ", "T");
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }
    }

    public record CursorKey(
            LocalDateTime cursorUpdatedAt,
            Long cursorSubscriberCount,
            Long idAfter
    ) {}
}