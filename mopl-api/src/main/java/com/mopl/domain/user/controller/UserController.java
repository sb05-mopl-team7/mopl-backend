package com.mopl.domain.user.controller;

import com.mopl.domain.user.dto.UserCreateRequest;
import com.mopl.domain.user.dto.UserDto;
import com.mopl.domain.user.dto.UserRoleUpdateRequest;
import com.mopl.domain.user.dto.UserSearchCondition;
import com.mopl.domain.user.service.UserService;
import com.mopl.global.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserDto> create(@RequestBody @Valid UserCreateRequest request) {
        UserDto userResponse = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/role")
    public ResponseEntity<Void> updateRole(@PathVariable Long userId, @Valid @RequestBody UserRoleUpdateRequest request){
        userService.updateRole(userId, request.role());
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<PageResponse<UserDto>> findAll(@ModelAttribute @Valid UserSearchCondition searchCondition){
        PageResponse<UserDto> results = userService.findAllUsers(searchCondition);
        return ResponseEntity.ok(results);
    }
}
