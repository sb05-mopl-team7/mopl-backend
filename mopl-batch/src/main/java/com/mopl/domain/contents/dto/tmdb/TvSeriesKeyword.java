package com.mopl.domain.contents.dto.tmdb;

import java.util.List;

public record TvSeriesKeyword(
        List<KeywordDto> genres
) {
}
