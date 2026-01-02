package com.mopl.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, max = 60, message = "비밀번호는 8자 이상 60자 이하여야 합니다")
        String password
) {
}
