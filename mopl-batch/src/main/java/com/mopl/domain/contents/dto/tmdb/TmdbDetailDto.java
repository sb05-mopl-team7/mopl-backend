package com.mopl.domain.contents.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TmdbDetailDto(
    Long id,
    String title,
    @JsonAlias("poster_path")
    @JsonProperty("thumbnailUrl")
    String thumbnailUrl,
    @JsonAlias("overview")
    @JsonProperty("description")
    String description,
    List<KeywordDto> genres
) {
}
