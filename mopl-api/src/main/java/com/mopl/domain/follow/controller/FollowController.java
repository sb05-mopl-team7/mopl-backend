package com.mopl.domain.follow.controller;

import com.mopl.domain.follow.controller.docs.FollowControllerDocs;
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
public class FollowController implements FollowControllerDocs {

    private final FollowService followService;

    // 팔로우 하기 API
    @Override
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