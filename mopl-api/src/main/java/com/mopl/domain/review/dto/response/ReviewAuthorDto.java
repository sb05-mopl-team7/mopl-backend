package com.mopl.domain.review.dto.response;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

public record ReviewAuthorDto(
        @JsonSerialize(using = ToStringSerializer.class)
        Long userId,
        String name,
        String profileImageUrl
) {
}