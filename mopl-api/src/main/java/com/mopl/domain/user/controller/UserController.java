package com.mopl.domain.user.controller;

import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.service.UserService;
import com.mopl.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<PageResponse<User>> findAll() {
        // 예시
        return null;
    }
}
