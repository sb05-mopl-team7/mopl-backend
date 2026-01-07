package com.mopl.domain.auth.service;

import com.mopl.domain.auth.dto.JwtDto;
import com.mopl.domain.auth.jwt.JwtTokenProvider;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.exception.UserErrorCode;
import com.mopl.domain.user.exception.UserException;
import com.mopl.domain.user.mapper.UserMapper;
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
    private final UserMapper userMapper;

    public JwtDto login(String username, String password, HttpServletResponse response) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_EXIST));

        if (!passwordEncoder.matches(password, user.getPassword()))
            throw new UserException(UserErrorCode.PASSWORD_NOT_CORRECT);

        // 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail(), user.getRole());

        JwtDto jwtDto = new JwtDto(userMapper.toDto(user), accessToken);

        // 쿠키로 응답
        addTokenCookie(response, "accessToken", accessToken, 60 * 60); //1시간
        addTokenCookie(response, "refreshToken", refreshToken, 60 * 60 * 24 * 14); //2주

        return jwtDto;
    }
    public void logout(HttpServletResponse response) {
        deleteCookie(response, "accessToken");
        deleteCookie(response, "refreshToken");
    }

    private void addTokenCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);       // JavaScript를 통한 XSS 공격 방지
        cookie.setSecure(false);         // HTTPS 환경에서만 전송 (운영 환경 권장)
        cookie.setPath("/");            // 모든 경로에서 쿠키 유효
        cookie.setMaxAge(maxAge);       // 쿠키 만료 시간 설정
        response.addCookie(cookie);
    }
    private void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setPath("/");
        cookie.setMaxAge(0);   // 즉시 만료
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

}
