package com.mopl.domain.auth.controller;

import com.mopl.domain.auth.dto.JwtDto;
import com.mopl.domain.auth.dto.ResetPasswordRequest;
import com.mopl.domain.auth.dto.SignInRequest;
import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.auth.service.AuthService;
import com.mopl.domain.auth.service.EmailService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/sign-in")
    public ResponseEntity<JwtDto> login(@ModelAttribute SignInRequest req, HttpServletResponse response) {
        JwtDto jwtDto = authService.login(req.username(), req.password(), response);
        return ResponseEntity.ok(jwtDto);
    }

    @PostMapping("/sign-out")
    public ResponseEntity<?> logout(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                    HttpServletResponse response) {
        Long myId = userPrincipal.getUserId();
        authService.logout(myId, response);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        emailService.resetPassword(req.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtDto> refresh(@CookieValue("REFRESH_TOKEN") String refreshToken, HttpServletResponse response) {
        JwtDto jwtDto = authService.refresh(refreshToken, response);
        return ResponseEntity.ok().body(jwtDto);
    }
    @GetMapping("/csrf-token")
    public CsrfToken getCsrfToken(CsrfToken token) {
        return token;
    }


}
