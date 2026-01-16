package com.mopl.domain.auth.service;

import com.mopl.domain.auth.dto.JwtDto;
import com.mopl.domain.auth.exception.AuthErrorCode;
import com.mopl.domain.auth.exception.AuthException;
import com.mopl.domain.auth.jwt.JwtTokenProvider;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.exception.UserErrorCode;
import com.mopl.domain.user.exception.UserException;
import com.mopl.domain.user.mapper.UserMapper;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.redis.RedisManager;
import com.mopl.global.redis.RedisNameSpace;
import com.mopl.global.s3.S3Manager;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final S3Manager s3Manager;
    private final RedisManager redisManager;

    @Value("${jwt.cookie.secure}")
    private boolean cookieSecure;

    @Value("${jwt.cookie.same-site:Lax}")  // 기본값 Lax
    private String cookieSameSite;

    public JwtDto login(String username, String password, HttpServletResponse response) {
        User user = userRepository.findByEmailAndLockedFalse(username)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_EXIST));

        //DB 비밀번호 일치
        if (passwordEncoder.matches(password, user.getPassword())) {
            return generateToken(user, response);
        }

        //임시 비밀번호 일치
        if (redisManager.hasKey(RedisNameSpace.TEMP_PASSWORD, username)) {
            Optional<String> tempPassword = redisManager.findByKey(
                    RedisNameSpace.TEMP_PASSWORD,
                    username,
                    String.class);

            if (tempPassword.isPresent() && tempPassword.get().equals(password)) {
                redisManager.delete(RedisNameSpace.TEMP_PASSWORD, username);
                return generateToken(user, response);
            }
        }
        //둘다 틀린 경우
        throw new UserException(UserErrorCode.PASSWORD_NOT_CORRECT);
    }

    private JwtDto generateToken(User user, HttpServletResponse response) {
        //TODO http는 controller로 분리 작업 필요
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        String thumbnailUrl = s3Manager.generatePresignedUrl(user.getProfileImageUrl());
        JwtDto jwtDto = new JwtDto(userMapper.toDto(user, thumbnailUrl), accessToken);

        addTokenCookie(response, "REFRESH_TOKEN", refreshToken, 60 * 60 * 24 * 14); //2주

        return jwtDto;
    }

    public void logout(HttpServletResponse response) {
        deleteCookie(response, "REFRESH_TOKEN");
    }

    public JwtDto refresh(String refreshToken, HttpServletResponse response) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_INVALID);
        }
        Long userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_EXIST));
        String newAccessToken = jwtTokenProvider.createAccessToken(user);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user);

        String thumbnailUrl = s3Manager.generatePresignedUrl(user.getProfileImageUrl());
        JwtDto jwtDto = new JwtDto(userMapper.toDto(user, thumbnailUrl), newAccessToken);

        addTokenCookie(response, "REFRESH_TOKEN", newRefreshToken, 60 * 60 * 24 * 14);

        return jwtDto;
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
