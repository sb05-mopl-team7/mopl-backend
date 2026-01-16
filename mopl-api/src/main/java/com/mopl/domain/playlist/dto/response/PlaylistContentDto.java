package com.mopl.domain.playlist.dto.response;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.util.List;

public record PlaylistContentDto(
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
