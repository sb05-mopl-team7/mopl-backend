package com.mopl.global.dto;

import com.mopl.global.SortDirection;
import lombok.Builder;

import java.util.List;

@Builder
public record PageResponse<T>(
        List<T> data,
        String nextCursor,
        Object nextIdAfter,
        boolean hasNext,
        long totalCount, // 0 고정값
        String sortBy,
        SortDirection sortDirection
) {
}
