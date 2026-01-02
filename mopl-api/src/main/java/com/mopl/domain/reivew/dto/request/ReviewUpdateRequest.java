package com.mopl.domain.reivew.dto.request;

public record ReviewUpdateRequest(
        String text,
        Double rating
) {
}