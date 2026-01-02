package com.mopl.domain.review.dto.response;

import com.mopl.global.SortDirection;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CursorResponseReviewDto {

    private List<ReviewDto> data;

    private String nextCursor;
    private Long nextIdAfter;

    private boolean hasNext;
    private long totalCount;

    private SortDirection sortDirection;
}
