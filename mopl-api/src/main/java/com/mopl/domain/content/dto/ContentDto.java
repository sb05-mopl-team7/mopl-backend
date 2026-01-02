package com.mopl.domain.content.dto;

import com.mopl.domain.content.enums.ContentType;
import lombok.Builder;

import java.util.List;

@Builder
public record ContentDto(
    Long id,
    ContentType type,
    String title,
    String description,
    String thumbnailUrl,
    List<String> tags,
    double averageRating,
    int reviewCount,
    int watchCount
) {
}
