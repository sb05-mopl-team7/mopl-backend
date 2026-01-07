package com.mopl.domain.review.dto.response;

import com.mopl.global.SortDirection;
import lombok.Builder;

import java.util.List;

@Builder
public record CursorResponseReviewDto(
        List<ReviewDto> data,
        String nextCursor,
        Long nextIdAfter,
        boolean hasNext,
        long totalCount,
        String sortBy,
        SortDirection sortDirection
) {
}
