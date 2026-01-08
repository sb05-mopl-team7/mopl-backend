package com.mopl.domain.auth.service;

import com.mopl.domain.auth.dto.JwtDto;
import com.mopl.domain.auth.jwt.JwtTokenProvider;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.exception.UserErrorCode;
import com.mopl.domain.user.exception.UserException;
import com.mopl.domain.user.mapper.UserMapper;
import com.mopl.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;

    @Value("${jwt.cookie.secure}")
    private boolean cookieSecure;

    @Value("${jwt.cookie.same-site:Lax}")  // 기본값 Lax
    private String cookieSameSite;

    public JwtDto login(String username, String password, HttpServletResponse response) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_EXIST));

        if (!passwordEncoder.matches(password, user.getPassword()))
            throw new UserException(UserErrorCode.PASSWORD_NOT_CORRECT);

        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        JwtDto jwtDto = new JwtDto(userMapper.toDto(user), accessToken);

        addTokenCookie(response, "ACCESS_TOKEN", accessToken, 60 * 60); //1시간
        addTokenCookie(response, "REFRESH_TOKEN", refreshToken, 60 * 60 * 24 * 14); //2주

        return jwtDto;
    }

    public void logout(HttpServletResponse response) {
        deleteCookie(response, "ACCESS_TOKEN");
        deleteCookie(response, "REFRESH_TOKEN");
    }

    private void addTokenCookie(HttpServletResponse response, String name, String value, int maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .maxAge(maxAge)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .path("/")
                .maxAge(0)// 즉시 만료
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

}
