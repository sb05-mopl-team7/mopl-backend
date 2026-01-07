package com.mopl.domain.follow.service;

import com.mopl.domain.follow.dto.request.FollowRequest;
import com.mopl.domain.follow.dto.response.FollowResponse;
import com.mopl.domain.follow.entity.Follow;
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

    @Transactional
    public FollowResponse follow(Long myId, FollowRequest request) {
        Long targetId = request.followeeId();

        // 1. 자기 자신 팔로우 방지 (400)
        if (myId.equals(targetId)) {
            throw new MoplException(ErrorCode.CANNOT_FOLLOW_SELF);
        }

        // 2. 이미 팔로우 중인지 확인 (400)
        if (followRepository.existsByFollowerIdAndFolloweeId(myId, targetId)) {
            throw new MoplException(ErrorCode.ALREADY_FOLLOWING);
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

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new MoplException(ErrorCode.NOT_FOUND));
    }
}