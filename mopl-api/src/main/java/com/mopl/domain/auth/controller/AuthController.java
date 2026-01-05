package com.mopl.domain.auth.controller;

import com.mopl.domain.auth.dto.SignInRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @GetMapping("sign-in")
    public ResponseEntity<?> signIn(@RequestBody SignInRequest request) {
        return ResponseEntity.status(200).build();
    }
}
