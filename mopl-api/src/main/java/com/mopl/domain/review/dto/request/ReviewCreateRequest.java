package com.mopl.domain.review.dto.request;

import jakarta.validation.constraints.*;

public record ReviewCreateRequest(
        @NotNull Long contentId,

        @NotBlank
        @Size(max = 255)
        String text,

        @DecimalMin("0.0")
        @DecimalMax("5.0")
        double rating
) {
}