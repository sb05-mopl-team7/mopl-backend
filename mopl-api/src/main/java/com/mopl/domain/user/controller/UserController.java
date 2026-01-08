package com.mopl.domain.user.controller;

import com.mopl.domain.user.dto.UserCreateRequest;
import com.mopl.domain.user.dto.UserDto;
import com.mopl.domain.user.dto.UserRoleUpdateRequest;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.enums.Role;
import com.mopl.domain.user.service.UserService;
import com.mopl.global.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public ResponseEntity<UserDto>create(@RequestBody @Valid UserCreateRequest request) {
        UserDto userResponse = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<?>updateRole(@PathVariable Long userId, UserRoleUpdateRequest request){
        Role userRole = userService.updateRole(userId, request);
        return ResponseEntity.ok().body(userRole);
    }
}
