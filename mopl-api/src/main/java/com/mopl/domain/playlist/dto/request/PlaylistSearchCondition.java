package com.mopl.domain.playlist.dto.request;

import com.mopl.domain.playlist.entity.Playlist;
import com.mopl.domain.playlist.exception.PlaylistErrorCode;
import com.mopl.domain.playlist.exception.PlaylistException;
import com.mopl.global.enums.SortDirection;
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
        // 기본값 세팅
        if (limit == null) limit = DEFAULT_LIMIT;
        if (sortDirection == null) sortDirection = SortDirection.DESCENDING;
        if (sortBy == null || sortBy.isBlank()) sortBy = "updatedAt";

        // sortBy 허용값 정규화 + 검증
        sortBy = normalizeSortBy(sortBy);

        // cursor / idAfter는 “쌍”으로만 허용
        boolean hasCursor = cursor != null && !cursor.isBlank();
        boolean hasIdAfter = idAfter != null;
        if (hasCursor != hasIdAfter) {
            throw new PlaylistException(PlaylistErrorCode.PLAYLIST_INVALID_REQUEST);
        }

        // cursor 형식 검증(여기서만 예외 변환)
        if (hasCursor) {
            validateCursor(cursor, sortBy);
        }
    }

    public CursorKey toCursorKey() {
        if (cursor == null || cursor.isBlank()) {
            return new CursorKey(null, null, null);
        }

        // 생성자에서 형식 검증을 이미 끝냈으므로 여기서는 "변환만" 수행
        if (isUpdatedAtSort()) {
            String normalized = cursor.trim().replace(" ", "T");
            return new CursorKey(LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME), null, idAfter);
        }

        return new CursorKey(null, Long.parseLong(cursor.trim()), idAfter);
    }

    public String nextCursorOf(Playlist last) {
        if (last == null) return null;

        return isUpdatedAtSort()
                ? last.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : String.valueOf(last.getSubscriberCount());
    }

    private boolean isUpdatedAtSort() {
        return "updatedAt".equalsIgnoreCase(sortBy);
    }

    private void validateCursor(String rawCursor, String normalizedSortBy) {
        try {
            if ("updatedAt".equalsIgnoreCase(normalizedSortBy)) {
                String normalized = rawCursor.trim().replace(" ", "T");
                LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return;
            }
            Long.parseLong(rawCursor.trim());
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new PlaylistException(PlaylistErrorCode.PLAYLIST_INVALID_REQUEST);
        }
    }

    private String normalizeSortBy(String raw) {
        String v = raw.trim();
        if ("updatedAt".equalsIgnoreCase(v)) return "updatedAt";
        if ("subscribeCount".equalsIgnoreCase(v) || "subscriberCount".equalsIgnoreCase(v)) return "subscriberCount";
        throw new PlaylistException(PlaylistErrorCode.PLAYLIST_INVALID_REQUEST);
    }

    public record CursorKey(
            LocalDateTime cursorUpdatedAt,
            Long cursorSubscriberCount,
            Long idAfter
    ) {}
}