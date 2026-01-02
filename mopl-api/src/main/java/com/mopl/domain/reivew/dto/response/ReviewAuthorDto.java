package com.mopl.domain.reivew.dto.response;

public record ReviewAuthorDto(
        Long userId,
        String name,
        String profileImageUrl
) {
}