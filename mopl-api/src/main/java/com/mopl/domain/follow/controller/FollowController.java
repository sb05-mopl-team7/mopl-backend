package com.mopl.domain.follow.controller;

import com.mopl.domain.follow.controller.docs.FollowControllerDocs;
import com.mopl.domain.follow.dto.request.FollowRequest;
import com.mopl.domain.follow.dto.response.FollowResponse;
import com.mopl.domain.follow.service.FollowService;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
public class FollowController implements FollowControllerDocs {

    private final FollowService followService;

    // 팔로우 하기 API
    @Override
    public ResponseEntity<FollowResponse> followUser(
            Principal principal,
            FollowRequest request
    ) {

        Long myId = Long.parseLong(principal.getName());

        FollowResponse response = followService.follow(myId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 언팔로우 하기 API
    @Override
    public ResponseEntity<Void> unfollowUser(
            Principal principal,
            Long followId
    ) {

        Long myId = Long.parseLong(principal.getName());

        // 서비스에 내 ID와 언팔로우할 ID를 넘긴다.
        followService.unfollow(myId, followId);

        return ResponseEntity.noContent().build();
    }
}