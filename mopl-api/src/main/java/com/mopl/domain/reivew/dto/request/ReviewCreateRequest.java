package com.mopl.domain.reivew.dto.request;

public record ReviewCreateRequest(
        Long contentId,
        String text,
        double rating
) {
}