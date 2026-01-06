package com.mopl.domain.auth.dto;

import com.mopl.domain.user.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtInformation {

    private UserDto userDto;
    private String accessToken;
    private String refreshToken;

    public void rotate(String newAccessToken, String newRefreshToken) {
        this.accessToken = newAccessToken;
        this.refreshToken = newRefreshToken;
    }
}
