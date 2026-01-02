package com.mopl.domain.user.dto;

import com.mopl.domain.user.enums.Role;

import java.time.LocalDateTime;

public record UserDto(
        Long id,
        LocalDateTime createdAt,
        String email,
        String name,
        String profileImageUrl,
        Role role,
        boolean locked
) {}
