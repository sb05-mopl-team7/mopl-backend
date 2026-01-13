package com.mopl.domain.follow.service;

import com.mopl.domain.follow.dto.request.FollowRequest;
import com.mopl.domain.follow.dto.response.FollowCountResponse;
import com.mopl.domain.follow.dto.response.FollowResponse;
import com.mopl.domain.follow.entity.Follow;
import com.mopl.domain.follow.exception.FollowErrorCode;
import com.mopl.domain.follow.exception.FollowException;
import com.mopl.domain.follow.repository.FollowRepository;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    // 팔로우 로직
    @Transactional
    public FollowResponse follow(Long myId, FollowRequest request) {
        Long targetId = request.followeeId();

        // 1. 자기 자신 팔로우 방지 (400)
        if (myId.equals(targetId)) {
            throw new FollowException(FollowErrorCode.CANNOT_FOLLOW_SELF);
        }

        // 2. 이미 팔로우 중인지 확인 (400)
        // // TODO: 동시성 이슈 체크할 것
        if (followRepository.existsByFollowerIdAndFolloweeId(myId, targetId)) {
            throw new FollowException(FollowErrorCode.ALREADY_FOLLOWING);
        }

        // 3. 사용자 조회 (404)
        User me = getUserOrThrow(myId);
        User target = getUserOrThrow(targetId);

        // 4. 팔로우 저장
        Follow follow = Follow.builder()
                .follower(me)
                .followee(target)
                .build();
        followRepository.save(follow);

        return FollowResponse.from(follow);

        // TODO 요구사항: "다른 사용자가 나를 팔로우하면 알림을 받습니다."
        // notificationService.send(target, NotificationType.FOLLOW, me.getName() + "님이 팔로우했습니다.");
    }

    // 언팔로우 로직
    @Transactional
    public void unfollow(Long myId, Long followId) {
        // 1. 팔로우 존재 확인 (404)
        Follow follow = followRepository.findById(followId)
                .orElseThrow(() -> new FollowException(FollowErrorCode.FOLLOW_NOT_FOUND));

        // 2. 언팔로우 권한 확인 (403)
        if (!follow.getFollower().getId().equals(myId)) {
            throw new FollowException(FollowErrorCode.NOT_YOUR_FOLLOW);
        }

        // 3. 언팔로우
        followRepository.delete(follow);
    }

    // 특정 유저 팔로우 확인 조회 로직
    public boolean isFollowing(Long followerId, Long followeeId) {
        return followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
    }

    // 팔로워/팔로잉 수 조회 로직
    public FollowCountResponse getFollowCounts(Long targetId) {

        // 1. 유저 존재 확인 (404)
        if (!userRepository.existsById(targetId)) {
            throw new MoplException(ErrorCode.NOT_FOUND);
        }

        // 나를 팔로우 하는 사람 카운트 조회 (Follower)
        long followerCount = followRepository.countByFolloweeId(targetId);

        // 내가 팔로우 하는 사람 카운트 조회 (Following)
        long followingCount = followRepository.countByFollowerId(targetId);

        return FollowCountResponse.of(followerCount, followingCount);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new MoplException(ErrorCode.NOT_FOUND));
    }
}