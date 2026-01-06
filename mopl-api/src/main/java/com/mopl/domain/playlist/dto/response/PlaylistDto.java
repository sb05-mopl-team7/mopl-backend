package com.mopl.domain.playlist.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PlaylistDto(
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
            Long userId,
            String name,
            String profileImageUrl
    ) {
    }

    public record Content(
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