package com.mopl.domain.contentchat.dto;

public record UserSummary(
    Long userId,
    String username,
    String profileImageUrl
) {
}