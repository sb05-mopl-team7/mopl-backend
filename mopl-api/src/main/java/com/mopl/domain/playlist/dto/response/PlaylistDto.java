package com.mopl.domain.playlist.dto.response;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;
import java.util.List;

public record PlaylistDto(
        @JsonSerialize(using = ToStringSerializer.class)
        Long id,
        Owner owner,
        String title,
        String description,
        LocalDateTime updatedAt,
        long subscriberCount,
        boolean subscribedByMe,
        List<Content> contents
) {

    public record Owner(
            @JsonSerialize(using = ToStringSerializer.class)
            Long userId,
            String name,
            String profileImageUrl
    ) {
    }

    public record Content(
            @JsonSerialize(using = ToStringSerializer.class)
            Long id,
            String type,
            String title,
            String description,
            String thumbnailUrl,
            List<String> tags,
            double averageRating,
            long reviewCount
    ) {
    }
}