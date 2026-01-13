package com.mopl.domain.follow.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "유저 팔로우/팔로잉 카운트 정보")
public record FollowCountResponse(
        @Schema(description = "팔로워 수 (나를 구독하는 사람)", example = "10")
        long followerCount,

        @Schema(description = "팔로잉 수 (내가 구독하는 사람)", example = "5")
        long followingCount
) {
    public static FollowCountResponse of(long followerCount, long followingCount) {
        return new FollowCountResponse(followerCount, followingCount);
    }
}