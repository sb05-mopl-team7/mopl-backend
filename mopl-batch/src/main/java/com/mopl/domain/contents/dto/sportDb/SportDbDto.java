package com.mopl.domain.contents.dto.sportDb;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SportDbDto(
        @JsonAlias("idEvent")
        Long id,
        @JsonAlias("strEvent")
        @JsonProperty("title")
        String title,
        @JsonAlias("strFilename")
        @JsonProperty("description")
        String description,
        @JsonAlias("strThumb")
        @JsonProperty("thumbnailUrl")
        String thumbnailUrl,
        String strVenue,
        String strSport
) {}
