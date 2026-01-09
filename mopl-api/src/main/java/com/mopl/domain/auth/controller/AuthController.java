package com.mopl.domain.auth.controller;

import com.mopl.domain.auth.dto.JwtDto;
import com.mopl.domain.auth.dto.SignInRequest;
import com.mopl.domain.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/sign-in")
    public ResponseEntity<JwtDto> login(@ModelAttribute SignInRequest req, HttpServletResponse response) {
        JwtDto jwtDto = authService.login(req.username(), req.password(), response);
        return ResponseEntity.ok(jwtDto);
    }

    @PostMapping("/sign-out")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.ok().build();
    }
}
