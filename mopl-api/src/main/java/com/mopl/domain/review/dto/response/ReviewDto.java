package com.mopl.domain.review.dto.response;

public record ReviewDto(
        Long id,
        Long contentId,
        ReviewAuthorDto author,
        String text,
        double rating
) {
}