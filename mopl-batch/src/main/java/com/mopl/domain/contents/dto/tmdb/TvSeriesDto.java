package com.mopl.domain.contents.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonAlias;

public record TvSeriesDto(
        Long id,
        @JsonAlias("name")
        String title,
        @JsonAlias("overview")
        String description,
        @JsonAlias("poster_path")
        String thumbnailUrl
) {
}
