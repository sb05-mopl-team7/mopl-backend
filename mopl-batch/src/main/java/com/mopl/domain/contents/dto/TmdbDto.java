package com.mopl.domain.contents.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TmdbDto(
        Long id,
        String title,
        String overview,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("release_date") String releaseDate,
        @JsonProperty("vote_average") Double voteAverage
) {}
