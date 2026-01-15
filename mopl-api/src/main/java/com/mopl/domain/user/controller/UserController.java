package com.mopl.domain.user.controller;

import com.mopl.domain.user.dto.*;
import com.mopl.domain.user.service.UserService;
import com.mopl.global.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @PatchMapping("/{userId}/locked")
    public ResponseEntity<Void> updateLocked(@PathVariable Long userId, @Valid @RequestBody UserLockUpdateRequest request){
        userService.updateLocked(userId, request.locked());
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<PageResponse<UserDto>> findAll(@ModelAttribute @Valid UserSearchCondition searchCondition){
        PageResponse<UserDto> results = userService.findAllUsers(searchCondition);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> findById(@PathVariable Long userId) {
        UserDto userDto = userService.detail(userId);
        return ResponseEntity.ok().body(userDto);
    }

    @PreAuthorize("principal.userId == #userId")
    @PatchMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDto> updateImage(@PathVariable Long userId,
                                               @RequestPart(value = "request", required = true) UserUpdateRequest request,
                                               @RequestPart(value = "image", required = false) MultipartFile image){
        UserDto updatedImage = userService.updateImage(userId, request.name(), image);
        return ResponseEntity.ok().body(updatedImage);
    }

    @PreAuthorize("principal.userId == #userId")
    @PatchMapping("/{userId}/password")
    public ResponseEntity<Void> updatePassword(@PathVariable Long userId,
                                               @Valid @RequestBody ChangePasswordRequest request){
        userService.updatePassword(userId, request.password());
        return ResponseEntity.ok().build();
    }
}
