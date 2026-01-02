package com.mopl.domain.user.dto;

import com.mopl.domain.user.enums.Role;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        String profileImageUrl,
        Role role,
        boolean locked,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
