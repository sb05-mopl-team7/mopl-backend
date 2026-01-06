package com.mopl.domain.auth.dto;

import com.mopl.domain.user.dto.UserDto;

public record JwtDto(
        UserDto userDto,
        String accessToken
) {
}
