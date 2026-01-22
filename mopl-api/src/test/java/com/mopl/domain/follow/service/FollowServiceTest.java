package com.mopl.domain.follow.service;

import com.mopl.domain.follow.dto.request.FollowRequest;
import com.mopl.domain.follow.dto.response.FollowResponse;
import com.mopl.domain.follow.entity.Follow;
import com.mopl.domain.follow.exception.FollowErrorCode;
import com.mopl.domain.follow.exception.FollowException;
import com.mopl.domain.follow.repository.FollowRepository;
import com.mopl.domain.notification.producer.NotificationEventProducer;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @InjectMocks
    private FollowService followService;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationEventProducer notificationEventProducer;

    private User createUser(Long id) {
        User user = new User("user" + id, "user" + id + "@test.com", "password");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Follow createFollow(Long id, User follower, User followee) {
        Follow follow = Follow.builder()
                .follower(follower)
                .followee(followee)
                .build();
        ReflectionTestUtils.setField(follow, "id", id);
        return follow;
    }

    @Nested
    @DisplayName("팔로우 실행")
    class FollowTest {

        @Test
        @DisplayName("[성공] 정상적인 팔로우 요청 시 데이터가 저장되고 알림이 발송된다")
        void success() {
            Long myId = 1L;
            Long targetId = 2L;
            FollowRequest request = new FollowRequest(targetId);

            User me = createUser(myId);
            User target = createUser(targetId);

            given(followRepository.existsByFollowerIdAndFolloweeId(myId, targetId)).willReturn(false);
            given(userRepository.findById(myId)).willReturn(Optional.of(me));
            given(userRepository.findById(targetId)).willReturn(Optional.of(target));
            given(followRepository.save(any(Follow.class))).willAnswer(invocation -> invocation.getArgument(0));

            FollowResponse response = followService.follow(myId, request);

            assertThat(response.followerId()).isEqualTo(myId);
            assertThat(response.followeeId()).isEqualTo(targetId);
            verify(followRepository, times(1)).save(any(Follow.class));
            verify(notificationEventProducer, times(1)).send(anyLong(), any(), any());
        }

        @Test
        @DisplayName("[예외] 자기 자신을 팔로우 할 수 없다")
        void fail_SelfFollow() {
            Long myId = 1L;
            FollowRequest request = new FollowRequest(myId);

            assertThatThrownBy(() -> followService.follow(myId, request))
                    .isInstanceOf(FollowException.class)
                    .hasMessage(FollowErrorCode.CANNOT_FOLLOW_SELF.getMessage());
        }

        @Test
        @DisplayName("[예외] 이미 팔로우 중인 경우 중복 팔로우가 불가능하다")
        void fail_AlreadyFollowing() {
            Long myId = 1L;
            Long targetId = 2L;
            FollowRequest request = new FollowRequest(targetId);

            given(followRepository.existsByFollowerIdAndFolloweeId(myId, targetId)).willReturn(true);

            assertThatThrownBy(() -> followService.follow(myId, request))
                    .isInstanceOf(FollowException.class)
                    .hasMessage(FollowErrorCode.ALREADY_FOLLOWING.getMessage());
        }
    }

    @Nested
    @DisplayName("언팔로우 실행")
    class UnFollowTest {

        @Test
        @DisplayName("[성공] 본인이 신청한 팔로우를 정상적으로 취소한다")
        void success() {
            Long myId = 1L;
            Long followId = 100L;

            User me = createUser(myId);
            User target = createUser(2L);
            Follow follow = createFollow(followId, me, target);

            given(followRepository.findById(followId)).willReturn(Optional.of(follow));

            followService.unFollow(myId, followId);

            verify(followRepository, times(1)).delete(follow);
        }

        @Test
        @DisplayName("[예외] 존재하지 않는 팔로우 ID로 언팔로우를 시도하면 에러를 던진다")
        void fail_FollowNotFound() {
            Long myId = 1L;
            Long followId = 999L;

            given(followRepository.findById(followId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> followService.unFollow(myId, followId))
                    .isInstanceOf(FollowException.class)
                    .hasMessage(FollowErrorCode.FOLLOW_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("[예외] 타인의 팔로우 관계를 삭제하려 하면 권한 에러를 던진다")
        void fail_NotYourFollow() {
            Long myId = 1L;
            Long otherUserId = 2L;
            Long followId = 100L;

            User otherUser = createUser(otherUserId);
            User target = createUser(3L);
            Follow follow = createFollow(followId, otherUser, target);

            given(followRepository.findById(followId)).willReturn(Optional.of(follow));

            assertThatThrownBy(() -> followService.unFollow(myId, followId))
                    .isInstanceOf(FollowException.class)
                    .hasMessage(FollowErrorCode.NOT_YOUR_FOLLOW.getMessage());
        }
    }

    @Nested
    @DisplayName("팔로우 카운트 조회")
    class CountTest {

        @Test
        @DisplayName("[성공] 프론트엔드 요구사항에 따라 팔로워 수를 Long 타입으로 반환한다")
        void success() {
            Long targetId = 1L;
            given(userRepository.existsById(targetId)).willReturn(true);
            given(followRepository.countByFolloweeId(targetId)).willReturn(15L);

            Long response = followService.getFollowCounts(targetId);

            assertThat(response).isEqualTo(15L);
        }

        @Test
        @DisplayName("[예외] 존재하지 않는 유저의 카운트 조회 시 404 에러를 던진다")
        void fail_UserNotFound() {
            Long nonExistentId = 999L;
            given(userRepository.existsById(nonExistentId)).willReturn(false);

            assertThatThrownBy(() -> followService.getFollowCounts(nonExistentId))
                    .isInstanceOf(MoplException.class)
                    .hasMessage(ErrorCode.NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("팔로우 여부 확인")
    class IsFollowingTest {

        @Test
        @DisplayName("[성공] 팔로우 관계가 존재하면 True를 반환한다")
        void trueCase() {
            given(followRepository.existsByFollowerIdAndFolloweeId(1L, 2L)).willReturn(true);
            assertThat(followService.isFollowing(1L, 2L)).isTrue();
        }

        @Test
        @DisplayName("[성공] 팔로우 관계가 없으면 False를 반환한다")
        void falseCase() {
            given(followRepository.existsByFollowerIdAndFolloweeId(anyLong(), anyLong())).willReturn(false);
            assertThat(followService.isFollowing(1L, 999L)).isFalse();
        }
    }
}