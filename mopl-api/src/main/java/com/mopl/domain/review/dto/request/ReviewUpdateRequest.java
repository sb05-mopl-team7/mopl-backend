package com.mopl.domain.review.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record ReviewUpdateRequest(
        @Size(max = 255)
        String text,

        @DecimalMin("0.0")
        @DecimalMax("5.0")
        Double rating
) {
}