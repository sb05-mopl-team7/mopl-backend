package com.mopl.domain.review.dto.request;

public record ReviewUpdateRequest(
        String text,
        Double rating
) {
}