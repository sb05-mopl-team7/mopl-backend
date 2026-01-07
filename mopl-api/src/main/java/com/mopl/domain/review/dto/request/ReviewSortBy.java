package com.mopl.domain.review.dto.request;

public enum ReviewSortBy {
    CREATED_AT("createdAt");

    private final String param;

    ReviewSortBy(String param) {
        this.param = param;
    }

    public String param() {
        return param;
    }
}
