package com.mopl.domain.user.dto;

import com.mopl.domain.user.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequest (
        @NotNull(message = "역할은 필수입니다")
        Role role
){
}
