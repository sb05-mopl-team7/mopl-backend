package com.mopl.domain.follow.controller;

import com.mopl.domain.follow.dto.request.FollowRequest;
import com.mopl.domain.follow.dto.response.FollowResponse;
import com.mopl.domain.follow.service.FollowService;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@Tag(name = "팔로우 관리", description = "사용자 팔로우 API")
@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @Operation(summary = "사용자 팔로우", description = "특정 사용자를 팔로우합니다. (자기 자신 팔로우 불가)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "팔로우 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (자기 자신, 이미 팔로우 중)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "사용자 찾을 수 없음")
    })

    // 팔로우 하기 API
    @PostMapping
    public ResponseEntity<@NonNull FollowResponse> followUser(
            Principal principal,
            @RequestBody @Valid FollowRequest request
    ) {
        if (principal == null) {
            throw new MoplException(ErrorCode.UNAUTHORIZED);
        }

        Long myId = Long.parseLong(principal.getName());

        FollowResponse response = followService.follow(myId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}