package com.mopl.domain.contents.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TmdbResponse<T>(
        int page,
        List<T> results,
        @JsonProperty("total_pages") int totalPages,
        @JsonProperty("total_results") int totalResults
) {
}
