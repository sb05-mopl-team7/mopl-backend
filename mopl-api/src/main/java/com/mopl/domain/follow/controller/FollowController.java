package com.mopl.domain.follow.controller;

import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.follow.controller.docs.FollowControllerDocs;
import com.mopl.domain.follow.dto.request.FollowRequest;
import com.mopl.domain.follow.dto.response.FollowResponse;
import com.mopl.domain.follow.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FollowController implements FollowControllerDocs {

    private final FollowService followService;

    // 팔로우 하기 API
    @Override
    public ResponseEntity<FollowResponse> followUser(
            @AuthenticationPrincipal UserPrincipal user,
            FollowRequest request
    ) {

        Long myId = user.getUserId();

        FollowResponse response = followService.follow(myId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 언팔로우 하기 API
    @Override
    public ResponseEntity<Void> unfollowUser(
            @AuthenticationPrincipal UserPrincipal user,
            Long followId
    ) {

        Long myId = user.getUserId();

        // 서비스에 내 ID와 언팔로우할 ID를 넘긴다.
        followService.unFollow(myId, followId);

        return ResponseEntity.noContent().build();
    }

    // 특정 유저 팔로우 확인 조회 API
    @Override
    @GetMapping("/followed-by-me")
    public ResponseEntity<Boolean> checkFollowStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam Long followeeId) {

        boolean result = followService.isFollowing(userPrincipal.getUserId(), followeeId);
        return ResponseEntity.ok(result);
    }

    // 팔로워/팔로잉 수 카운트 조회 API
    @Override
    @GetMapping("/count")
    public ResponseEntity<Long> getFollowCounts(@RequestParam Long followeeId) {

        Long response = followService.getFollowCounts(followeeId);

        return ResponseEntity.ok(response);
    }
}