package com.mopl.domain.user.dto;

import com.mopl.domain.user.enums.Role;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserResponse {
    private Long id;
    private LocalDateTime createdAt;
    private String email;
    private String name;
    private String profileImageUrl;
    private Role role;
    private boolean locked;
}
