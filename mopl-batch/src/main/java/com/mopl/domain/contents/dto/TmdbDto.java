package com.mopl.domain.contents.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record TmdbDto(
        Long id,
        String title,
        @JsonAlias("overview") // 읽을 때
        @JsonProperty("description")
        String description,
        @JsonAlias("poster_path") // 읽을 때
        @JsonProperty("thumbnailUrl")
        String thumbnailUrl,
        Double averageRating,
        Integer reviewCount
) {}
