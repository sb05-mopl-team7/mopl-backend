package com.mopl.domain.follow.service;

import com.mopl.domain.follow.dto.request.FollowRequest;
import com.mopl.domain.follow.dto.response.FollowResponse;
import com.mopl.domain.follow.entity.Follow;
import com.mopl.domain.follow.exception.FollowErrorCode;
import com.mopl.domain.follow.exception.FollowException;
import com.mopl.domain.follow.repository.FollowRepository;
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

    // 테스트 유저 데이터 생성 헬퍼 메서드
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

    // 1. 팔로우 로직 테스트
    @Nested
    @DisplayName("팔로우 (follow)")
    class FollowTest {

        @Test
        @DisplayName("[성공] 정상적인 팔로우 요청")
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
        }

        @Test
        @DisplayName("[실패] 자기 자신을 팔로우 할 수 없음 (CANNOT_FOLLOW_SELF)")
        void fail_SelfFollow() {
            Long myId = 1L;
            FollowRequest request = new FollowRequest(myId);

            assertThatThrownBy(() -> followService.follow(myId, request))
                    .isInstanceOf(FollowException.class)
                    .hasMessage(FollowErrorCode.CANNOT_FOLLOW_SELF.getMessage());
        }

        @Test
        @DisplayName("[실패] 이미 팔로우한 사용자 (ALREADY_FOLLOWING)")
        void fail_AlreadyFollowing() {
            Long myId = 1L;
            Long targetId = 2L;
            FollowRequest request = new FollowRequest(targetId);

            given(followRepository.existsByFollowerIdAndFolloweeId(myId, targetId)).willReturn(true);

            assertThatThrownBy(() -> followService.follow(myId, request))
                    .isInstanceOf(FollowException.class)
                    .hasMessage(FollowErrorCode.ALREADY_FOLLOWING.getMessage());
        }

        @Test
        @DisplayName("[실패] 요청자(나)를 찾을 수 없음 (NOT_FOUND)")
        void fail_MeNotFound() {
            Long myId = 1L;
            Long targetId = 2L;
            FollowRequest request = new FollowRequest(targetId);

            given(followRepository.existsByFollowerIdAndFolloweeId(myId, targetId)).willReturn(false);
            given(userRepository.findById(myId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> followService.follow(myId, request))
                    .isInstanceOf(MoplException.class)
                    .hasMessage(ErrorCode.NOT_FOUND.getMessage());
        }
    }


    // 2. 언팔로우 로직 테스트
    @Nested
    @DisplayName("언팔로우 (unFollow)")
    class UnFollowTest {

        @Test
        @DisplayName("[성공] 본인의 팔로우 취소 성공")
        void success() {
            Long myId = 1L;
            Long targetId = 2L;
            Long followId = 100L;

            User me = createUser(myId);
            User target = createUser(targetId);
            Follow follow = createFollow(followId, me, target);

            given(followRepository.findById(followId)).willReturn(Optional.of(follow));

            followService.unFollow(myId, followId);

            verify(followRepository, times(1)).delete(follow);
        }

        @Test
        @DisplayName("[실패] 남의 팔로우를 취소하려고 함 (NOT_YOUR_FOLLOW)")
        void fail_NotYourFollow() {
            Long myId = 1L;
            Long realOwnerId = 2L;
            Long targetId = 3L;
            Long followId = 100L;

            User realOwner = createUser(realOwnerId);
            User target = createUser(targetId);
            Follow follow = createFollow(followId, realOwner, target);

            given(followRepository.findById(followId)).willReturn(Optional.of(follow));

            assertThatThrownBy(() -> followService.unFollow(myId, followId))
                    .isInstanceOf(FollowException.class)
                    .hasMessage(FollowErrorCode.NOT_YOUR_FOLLOW.getMessage());
        }
    }

    // 3. 팔로우 카운트 조회 로직 테스트
    @Nested
    @DisplayName("팔로우 카운트 조회 (getFollowCounts)")
    class CountTest {

        @Test
        @DisplayName("[성공] 팔로워 카운트 조회 성공")
        void success() {
            // Given
            Long targetId = 1L;
            given(userRepository.existsById(targetId)).willReturn(true);
            given(followRepository.countByFolloweeId(targetId)).willReturn(15L);
            given(followRepository.countByFollowerId(targetId)).willReturn(3L);

            // 서비스 반환 타입인 Long에 맞게 변수 타입 수정
            Long response = followService.getFollowCounts(targetId);

            // 서비스 로직 상 최종적으로 followerCount(15L)를 반환하므로 해당 값 검증
            assertThat(response).isEqualTo(15L);
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 유저의 카운트 조회 (NOT_FOUND)")
        void fail_UserNotFound() {
            Long nonExistentId = 999L;
            given(userRepository.existsById(nonExistentId)).willReturn(false);

            assertThatThrownBy(() -> followService.getFollowCounts(nonExistentId))
                    .isInstanceOf(MoplException.class)
                    .hasMessage(ErrorCode.NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("[실패] TargetId가 Null일 경우 예외 발생")
        void fail_NullId() {
            // isNull() 매처를 사용하여 IDE 경고 우회
            given(userRepository.existsById(isNull())).willThrow(new IllegalArgumentException("ID cannot be null"));

            assertThatThrownBy(() -> followService.getFollowCounts(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // 4. 팔로우 여부 확인 로직 테스트
    @Nested
    @DisplayName("팔로우 여부 확인 (isFollowing)")
    class IsFollowingTest {

        @Test
        @DisplayName("[성공] 팔로우 중이면 True 반환")
        void trueCase() {
            given(followRepository.existsByFollowerIdAndFolloweeId(1L, 2L)).willReturn(true);
            assertThat(followService.isFollowing(1L, 2L)).isTrue();
        }

        @Test
        @DisplayName("[성공] 존재하지 않는 유저 ID로 조회 시 False 반환")
        void check_NonExistentUser_ReturnsFalse() {
            given(followRepository.existsByFollowerIdAndFolloweeId(anyLong(), anyLong())).willReturn(false);
            assertThat(followService.isFollowing(1L, 999L)).isFalse();
        }

        @Test
        @DisplayName("[실패] ID가 Null일 경우 예외 발생")
        void fail_NullId() {
            // eq(1L)와 isNull()을 조합하여 모든 인자에 매처 적용
            given(followRepository.existsByFollowerIdAndFolloweeId(eq(1L), isNull()))
                    .willThrow(new IllegalArgumentException("ID cannot be null"));

            assertThatThrownBy(() -> followService.isFollowing(1L, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}