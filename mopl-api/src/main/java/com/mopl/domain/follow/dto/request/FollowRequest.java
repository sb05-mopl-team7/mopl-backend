package com.mopl.domain.follow.dto.request;

import jakarta.validation.constraints.NotNull;

public record FollowRequest(
        @NotNull(message = "팔로우할 대상의 ID는 필수입니다.")
        Long followeeId
) {
}
