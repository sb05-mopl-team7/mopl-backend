package com.mopl.domain.review.dto.request;

public record ReviewCreateRequest(
        Long contentId,
        String text,
        double rating
) {
}