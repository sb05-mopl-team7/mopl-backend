package com.mopl.domain.user.dto.response;

public record UserSummaryDto(
        Long userId,
        String name,
        String profileImageUrl
) {
}
