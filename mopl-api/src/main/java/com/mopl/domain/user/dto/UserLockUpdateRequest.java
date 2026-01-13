package com.mopl.domain.user.dto;

import jakarta.validation.constraints.NotNull;

public record UserLockUpdateRequest(
        @NotNull(message = "잠금 여부는 필수 입력값입니다.")
        Boolean locked
) {
}
