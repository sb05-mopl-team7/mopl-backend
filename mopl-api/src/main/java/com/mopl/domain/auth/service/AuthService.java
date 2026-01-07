package com.mopl.domain.auth.service;

import com.mopl.domain.auth.jwt.JwtTokenProvider;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.exception.UserErrorCode;
import com.mopl.domain.user.exception.UserException;
import com.mopl.domain.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public String login(String username, String password, HttpServletResponse response) {
        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_EXIST));

        if(!passwordEncoder.matches(password, user.getPassword()))
            throw new UserException(UserErrorCode.PASSWORD_NOT_CORRECT);

        // 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail(), user.getRole());

        // 쿠키로 응답
        addTokenCookie(response, "refreshToken", refreshToken, (int) (jwtTokenProvider.getRefreshTokenValidity() / 1000));
        return accessToken;
    }

    private void addTokenCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);       // JavaScript를 통한 XSS 공격 방지
        cookie.setSecure(false);         // HTTPS 환경에서만 전송 (운영 환경 권장)
        cookie.setPath("/");            // 모든 경로에서 쿠키 유효
        cookie.setMaxAge(maxAge);       // 쿠키 만료 시간 설정
        response.addCookie(cookie);
    }
}
