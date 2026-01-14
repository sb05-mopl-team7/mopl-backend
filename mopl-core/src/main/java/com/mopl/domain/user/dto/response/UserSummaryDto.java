package com.mopl.domain.user.dto.response;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

public record UserSummaryDto(
        @JsonSerialize(using = ToStringSerializer.class)
        Long userId,
        String name,
        String profileImageUrl
) {
}
