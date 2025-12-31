package com.mopl.domain.contents.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SportDbDto(
        Long id,
        @JsonAlias("strEvent")
        @JsonProperty("title")
        String title,
        @JsonAlias("strFilename")
        @JsonProperty("description")
        String description,
        @JsonAlias("strThumb")
        @JsonProperty("thumbnailUrl")
        String thumbnailUrl
) {}
