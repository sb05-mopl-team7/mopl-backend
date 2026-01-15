package com.mopl.domain.follow.dto.response;

import com.mopl.domain.follow.entity.Follow;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

public record FollowResponse(
        @JsonSerialize(using = ToStringSerializer.class)
        Long id,

        @JsonSerialize(using = ToStringSerializer.class)
        Long followerId,

        @JsonSerialize(using = ToStringSerializer.class)
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