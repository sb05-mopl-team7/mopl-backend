package com.mopl.domain.follow.controller;

import com.mopl.domain.follow.controller.docs.FollowControllerDocs;
import com.mopl.domain.follow.dto.request.FollowRequest;
import com.mopl.domain.follow.dto.response.FollowResponse;
import com.mopl.domain.follow.service.FollowService;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

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

    // 언팔로우 하기 API
    @Override
    @DeleteMapping("/{followId}")
    public ResponseEntity<@NonNull Void> unfollowUser(
            Principal principal,
            @PathVariable Long followId
    ) {
        if (principal == null) {
            throw new MoplException(ErrorCode.UNAUTHORIZED);
        }

        Long myId = Long.parseLong(principal.getName());

        // 서비스에 내 ID와 삭제할 팔로우 ID를 넘깁니다.
        followService.unfollow(myId, followId);

        return ResponseEntity.noContent().build();
    }
}