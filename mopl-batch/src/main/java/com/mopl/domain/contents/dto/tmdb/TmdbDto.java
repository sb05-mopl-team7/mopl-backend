package com.mopl.domain.contents.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record TmdbDto(
        Long id,
        String title,
        @JsonAlias("overview")
        @JsonProperty("description")
        String description,
        @JsonAlias("poster_path")
        @JsonProperty("thumbnailUrl")
        String thumbnailUrl,
        Double averageRating,
        Integer reviewCount
) {
}
