package com.mopl.domain.auth.controller;

import com.mopl.domain.auth.dto.JwtDto;
import com.mopl.domain.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/sign-in")
    public ResponseEntity<JwtDto> login(@RequestParam String username,
                                        @RequestParam String password,
                                        HttpServletResponse response) {
        JwtDto jwtDto = authService.login(username, password, response);
        return ResponseEntity.ok(jwtDto);
    }
}
