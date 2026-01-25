package com.mopl.domain.auth.controller;

import com.mopl.domain.auth.dto.*;
import com.mopl.domain.auth.service.AuthService;
import com.mopl.domain.auth.service.EmailService;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    @Value("${jwt.cookie.secure}")
    private boolean cookieSecure;

    @Value("${jwt.cookie.same-site:None}")  // 기본값 Lax
    private String cookieSameSite;

    private static final int REFRESH_TOKEN_MAX_AGE = 60 * 60 * 24 * 14; // 2주

    @PostMapping("/sign-in")
    public ResponseEntity<JwtDto> login(@ModelAttribute SignInRequest req,
                                        HttpServletResponse response) {
        TokenResultDto tokenDto = authService.login(req.username(), req.password());
        addTokenCookie(response, "REFRESH_TOKEN", tokenDto.refreshToken(), REFRESH_TOKEN_MAX_AGE);
        return ResponseEntity.ok(tokenDto.jwtDto());
    }

    @PostMapping("/sign-out")
    public ResponseEntity<?> logout(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                    HttpServletResponse response) {
        Long myId = userPrincipal.getUserId();
        authService.logout(myId);
        deleteCookie(response, "REFRESH_TOKEN");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest req) {
        emailService.resetPassword(req.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtDto> refresh(@CookieValue(value = "REFRESH_TOKEN", required = false) String refreshToken,
                                          HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new MoplException(ErrorCode.UNAUTHORIZED);
        }
        TokenResultDto tokenDto = authService.refresh(refreshToken);
        addTokenCookie(response, "REFRESH_TOKEN", tokenDto.refreshToken(), REFRESH_TOKEN_MAX_AGE);
        return ResponseEntity.ok().body(tokenDto.jwtDto());
    }

    @GetMapping("/csrf-token")
    public CsrfToken getCsrfToken(CsrfToken token) {
        return token;
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
