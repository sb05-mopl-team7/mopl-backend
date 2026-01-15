package com.mopl.domain.playlist.dto.response;

import com.mopl.domain.user.dto.response.UserSummaryDto;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;
import java.util.List;

public record PlaylistDto(
        @JsonSerialize(using = ToStringSerializer.class)
        Long id,
        UserSummaryDto owner,
        String title,
        String description,
        LocalDateTime updatedAt,
        long subscriberCount,
        boolean subscribedByMe,
        List<PlaylistContentDto> contents
) {
}