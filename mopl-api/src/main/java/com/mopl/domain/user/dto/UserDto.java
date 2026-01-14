package com.mopl.domain.user.dto;

import com.mopl.domain.user.enums.Role;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;

public record UserDto(
        @JsonSerialize(using = ToStringSerializer.class)
        Long id,
        LocalDateTime createdAt,
        String email,
        String name,
        String profileImageUrl,
        Role role,
        boolean locked
) {}
