package com.mopl.domain.follow.controller.docs;

import com.mopl.domain.follow.dto.request.FollowRequest;
import com.mopl.domain.follow.dto.response.FollowResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import java.security.Principal;

public interface FollowControllerDocs {

    // 팔로우 명세서
    @Operation(summary = "사용자 팔로우", description = "특정 사용자를 팔로우합니다. (자기 자신 팔로우 불가)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "팔로우 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (자기 자신, 이미 팔로우 중)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "사용자 찾을 수 없음")
    })
    ResponseEntity<@NonNull FollowResponse> followUser(
            Principal principal,
            FollowRequest request
    );
}