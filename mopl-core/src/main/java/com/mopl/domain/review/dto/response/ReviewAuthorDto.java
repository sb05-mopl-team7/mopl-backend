package com.mopl.domain.review.dto.response;

public record ReviewAuthorDto(
        Long userId,
        String name,
        String profileImageUrl
) {
}