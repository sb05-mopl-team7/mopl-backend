package com.mopl.domain.review.dto.response;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

public record ReviewDto(
        @JsonSerialize(using = ToStringSerializer.class)
        Long id,
        @JsonSerialize(using = ToStringSerializer.class)
        Long contentId,
        ReviewAuthorDto author,
        String text,
        double rating
) {
}