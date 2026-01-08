package com.mopl.domain.follow.dto.response;

import com.mopl.domain.follow.entity.Follow;

public record FollowResponse(
        Long id,
        Long followerId,
        Long followeeId
) {
    public static FollowResponse from(Follow follow) {

        return new FollowResponse(
                follow.getId(),
                follow.getFollower().getId(),
                follow.getFollowee().getId()
        );
    }
}