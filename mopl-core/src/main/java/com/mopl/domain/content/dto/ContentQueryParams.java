package com.mopl.domain.content.dto;

import com.mopl.global.enums.SortDirection;

import java.util.List;

public record ContentQueryParams(
    String typeEqual,
    String keywordLike,
    List<String> tagsIn,
    String cursor,
    String idAfter,
    Integer limit,
    SortDirection sortDirection,
    String sortBy
) {
}
