package com.mopl.domain.watching.dto.response;

import com.mopl.domain.content.enums.ContentType;
import lombok.Builder;

import java.util.Collections;
import java.util.List;

public record ContentPayload(
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
    @Builder
    public ContentPayload {
        type = null;
        title = "";
        description = "";
        thumbnailUrl = "";
        tags = Collections.emptyList();
        averageRating = 0.0;
        reviewCount = 0;
        watcherCount = 0;
    }
}