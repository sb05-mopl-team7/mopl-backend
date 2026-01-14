package com.mopl.domain.auth.dto;

import com.mopl.domain.user.enums.Role;

public record UserPrincipal(
        Long userId,
        Role role
) {
}
