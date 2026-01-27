package com.mopl.domain.content.dto;

import com.mopl.domain.content.enums.ContentType;

import java.util.List;

public record ContentDto(
    String id,
    ContentType type,
    String title,
    String description,
    String thumbnailUrl,
    List<String> tags,
    double averageRating,
    int reviewCount,
    int watcherCount
) {
}
