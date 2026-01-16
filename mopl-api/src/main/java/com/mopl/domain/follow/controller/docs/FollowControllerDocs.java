package com.mopl.domain.follow.controller.docs;

import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.follow.dto.request.FollowRequest;
import com.mopl.domain.follow.dto.response.FollowResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "팔로우 관리", description = "사용자 팔로우 API")
@RequestMapping("/api/follows")
public interface FollowControllerDocs {

    // 팔로우 명세서
    @Operation(summary = "사용자 팔로우", description = "특정 사용자를 팔로우합니다. (자기 자신 팔로우 불가)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "팔로우 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (자기 자신, 이미 팔로우 중)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "사용자 찾을 수 없음")
    })
    @PostMapping
    ResponseEntity<FollowResponse> followUser(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody FollowRequest request
    );

    // 언팔로우 명세서
    @Operation(summary = "팔로우 취소", description = "신청했던 팔로우를 취소(삭제)합니다. 본인의 팔로우만 삭제 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공 (반환값 없음)"),
            @ApiResponse(responseCode = "204", description = "성공 (삭제 완료)"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (ID 형식 오류 등)"),
            @ApiResponse(responseCode = "401", description = "인증 오류 (로그인 필요)"),
            @ApiResponse(responseCode = "403", description = "권한 오류 (본인의 팔로우가 아님)"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음 (이미 삭제되었거나 존재하지 않음)"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @DeleteMapping("/{followId}")
    ResponseEntity<Void> unfollowUser(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal user,
            @Parameter(description = "삭제할 팔로우 ID (PK)") @PathVariable Long followId
    );

    // 특정 유저 팔로우 확인 조회 명세서
    @Operation(summary = "팔로우 여부 확인", description = "로그인한 사용자가 특정 사용자(followeeId)를 현재 팔로우하고 있는지 여부를 조회합니다. (true: 팔로우 중, false: 미팔로우)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (true/false 반환)"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (파라미터 누락 또는 잘못된 형식)"),
            @ApiResponse(responseCode = "401", description = "인증 오류 (로그인 필요)"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/followed-by-me")
    ResponseEntity<Boolean> checkFollowStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal user,
            @Parameter(description = "확인할 상대방 사용자 ID (PK)", required = true) @RequestParam Long followeeId
    );

    // 팔로워/팔로잉 수 조회 명세서
    @Operation(summary = "팔로워/팔로잉 수 조회", description = "특정 사용자(targetId)의 팔로워 수(나를 구독)와 팔로잉 수(내가 구독)를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (파라미터 누락 등)"),
            @ApiResponse(responseCode = "401", description = "인증 오류(로그인 필요)"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/count")
    ResponseEntity<Long> getFollowCounts(
            @Parameter(description = "조회할 대상 사용자 ID (PK)", required = true) @RequestParam Long targetId
    );



}

