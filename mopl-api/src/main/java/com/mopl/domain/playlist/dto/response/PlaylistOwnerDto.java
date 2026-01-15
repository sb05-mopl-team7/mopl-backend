package com.mopl.domain.playlist.dto.response;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

public record PlaylistOwnerDto(
        @JsonSerialize(using = ToStringSerializer.class)
        Long userId,
        String name,
        String profileImageUrl
) {
}