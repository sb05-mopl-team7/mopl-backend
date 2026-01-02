package com.mopl.domain.reivew.dto.response;

public record ReviewDto(
        Long id,
        Long contentId,
        ReviewAuthorDto author,
        String text,
        double rating
) {
}