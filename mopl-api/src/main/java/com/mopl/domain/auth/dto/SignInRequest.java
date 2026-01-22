package com.mopl.domain.auth.dto;

public record SignInRequest(
        String username,
        String password) {
}
