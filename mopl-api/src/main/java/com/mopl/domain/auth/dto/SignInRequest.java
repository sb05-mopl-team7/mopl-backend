package com.mopl.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignInRequest(
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "유효한 이메일 형식이어야 합니다")
        @Size(max = 100, message = "이메일은 100자 이하여야 합니다")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, max = 60, message = "비밀번호는 8자 이상 60자 이하여야 합니다")
        String password
) {
}
