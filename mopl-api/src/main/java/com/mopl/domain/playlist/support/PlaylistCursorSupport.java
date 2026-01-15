package com.mopl.domain.playlist.support;

import com.mopl.domain.playlist.entity.Playlist;
import com.mopl.global.enums.SortDirection;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class PlaylistCursorSupport {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private PlaylistCursorSupport() {}

    public static int normalizeLimit(Integer limit) {
        if (limit == null) return DEFAULT_LIMIT;
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }
        return limit;
    }

    public static String normalizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return "updatedAt";

        String v = sortBy.trim();
        if ("updatedAt".equalsIgnoreCase(v)) return "updatedAt";
        if ("subscribeCount".equalsIgnoreCase(v) || "subscriberCount".equalsIgnoreCase(v)) {
            return "subscriberCount";
        }
        throw new MoplException(ErrorCode.INVALID_REQUEST);
    }

    public static SortDirection normalizeDirection(SortDirection sortDirection) {
        return (sortDirection == null) ? SortDirection.DESCENDING : sortDirection;
    }

    public static CursorKey parseCursorKey(String cursorRaw, String idAfterRaw, String normalizedSortBy) {
        boolean hasCursor = cursorRaw != null && !cursorRaw.isBlank();
        boolean hasIdAfter = idAfterRaw != null && !idAfterRaw.isBlank();

        if (hasCursor != hasIdAfter) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }
        if (!hasCursor) {
            return new CursorKey(null, null, null);
        }

        Long parsedIdAfter = parseLong(idAfterRaw);

        if ("updatedAt".equalsIgnoreCase(normalizedSortBy)) {
            LocalDateTime updatedAt = parseDateTimeCursor(cursorRaw);
            return new CursorKey(updatedAt, null, parsedIdAfter);
        } else {
            Long subscriberCount = parseLong(cursorRaw);
            return new CursorKey(null, subscriberCount, parsedIdAfter);
        }
    }

    public static String nextCursor(String normalizedSortBy, Playlist last) {
        if (last == null) return null;
        if ("updatedAt".equalsIgnoreCase(normalizedSortBy)) {
            return formatDateTimeCursor(last.getUpdatedAt());
        }
        return String.valueOf(last.getSubscriberCount());
    }

    private static Long parseLong(String raw) {
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

    private static String formatDateTimeCursor(LocalDateTime value) {
        return value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public record CursorKey(
            LocalDateTime cursorUpdatedAt,
            Long cursorSubscriberCount,
            Long idAfter
    ) {}
}