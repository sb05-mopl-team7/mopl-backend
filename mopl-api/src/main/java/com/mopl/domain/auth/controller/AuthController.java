package com.mopl.domain.auth.controller;

import com.mopl.domain.auth.dto.JwtDto;
import com.mopl.domain.auth.dto.LoginDto;
import com.mopl.domain.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sign-in")
    public ResponseEntity<JwtDto> login(@RequestBody LoginDto req, HttpServletResponse response) {
        JwtDto jwtDto = authService.login(req.username(), req.password(), response);
        return ResponseEntity.ok(jwtDto);
    }
}
