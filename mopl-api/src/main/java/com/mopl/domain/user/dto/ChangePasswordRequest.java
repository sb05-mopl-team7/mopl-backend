package com.mopl.domain.user.dto;

import jakarta.validation.constraints.NotNull;

public record ChangePasswordRequest (
        @NotNull(message = "비밀번호는 필수 입력값입니다.")
        String password
){
}
