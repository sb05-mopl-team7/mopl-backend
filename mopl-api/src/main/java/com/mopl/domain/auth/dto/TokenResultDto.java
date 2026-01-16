package com.mopl.domain.auth.dto;

public record TokenResultDto(
        JwtDto jwtDto,
        String refreshToken
) {
}
