package com.mopl.domain.watching.service;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.enums.ContentType;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.domain.watching.dto.response.WatchingSessionResponse;
import com.mopl.domain.watching.entity.WatchingSession;
import com.mopl.domain.watching.exception.WatchingErrorCode;
import com.mopl.domain.watching.exception.WatchingException;
import com.mopl.domain.watching.repository.WatchingSessionRepository;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WatchingSessionServiceTest {

    @InjectMocks
    private WatchingSessionService watchingSessionService;

    @Mock
    private WatchingSessionRepository watchingSessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContentRepository contentRepository;

    // 테스트 데이터 생성 헬퍼 메서드 (FollowServiceTest 스타일)
    private User createUser(Long id) {
        User user = new User("user" + id, "user" + id + "@test.com", "password");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Content createContent(Long id) {
        Content content = new Content(ContentType.movie, "인터스텔라", "우주 영화", "https://image.com/thumb.jpg");
        ReflectionTestUtils.setField(content, "id", id);
        return content;
    }

    // 1. 시청 세션 조회 로직 테스트
    @Nested
    @DisplayName("시청 세션 조회 (getWatchingSession)")
    class GetWatchingSessionTest {

        @Test
        @DisplayName("[성공] 사용자가 시청 중인 세션이 있으면 상세 정보를 반환한다")
        void success() {
            // Given
            Long watcherId = 1L;
            Long contentId = 100L;
            User user = createUser(watcherId);
            Content content = createContent(contentId);
            WatchingSession session = WatchingSession.builder()
                    .id(watcherId)
                    .contentId(contentId)
                    .build();

            given(userRepository.existsById(watcherId)).willReturn(true);
            given(watchingSessionRepository.findById(watcherId)).willReturn(Optional.of(session));
            given(userRepository.findById(watcherId)).willReturn(Optional.of(user));
            given(contentRepository.findById(contentId)).willReturn(Optional.of(content));

            // When
            WatchingSessionResponse response = watchingSessionService.getWatchingSession(watcherId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(String.valueOf(watcherId));
            assertThat(response.watcher().name()).isEqualTo(user.getName());
            assertThat(response.content().title()).isEqualTo(content.getTitle());
        }

        @Test
        @DisplayName("[성공] 사용자가 시청 중인 세션이 없으면 Null을 반환한다")
        void success_ReturnNull() {
            // Given
            Long watcherId = 1L;
            given(userRepository.existsById(watcherId)).willReturn(true);
            given(watchingSessionRepository.findById(watcherId)).willReturn(Optional.empty());

            // When
            WatchingSessionResponse response = watchingSessionService.getWatchingSession(watcherId);

            // Then
            assertThat(response).isNull();
        }

        // 2. 비즈니스 예외 케이스 (400)
        @Test
        @DisplayName("[실패] 잘못된 요청 파라미터(음수 ID) 시 INVALID_WATCHING_REQUEST 예외 발생")
        void fail_InvalidRequest() {
            // Given
            Long invalidId = -1L;

            // When & Then
            assertThatThrownBy(() -> watchingSessionService.getWatchingSession(invalidId))
                    .isInstanceOf(WatchingException.class)
                    .hasMessage(WatchingErrorCode.INVALID_WATCHING_REQUEST.getMessage());
        }

        // 3. 리소스 없음 케이스 (404)
        @Test
        @DisplayName("[실패] 존재하지 않는 사용자를 조회하면 USER_NOT_FOUND 예외 발생")
        void fail_UserNotFound() {
            // Given
            Long watcherId = 999L;
            given(userRepository.existsById(watcherId)).willReturn(false);

            // When & Then
            assertThatThrownBy(() -> watchingSessionService.getWatchingSession(watcherId))
                    .isInstanceOf(WatchingException.class)
                    .hasMessage(WatchingErrorCode.USER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("[실패] 세션은 있으나 유저 정보를 찾을 수 없음 (USER_NOT_FOUND)")
        void fail_UserDetailNotFound() {
            // Given
            Long watcherId = 1L;
            WatchingSession session = WatchingSession.builder().id(watcherId).contentId(100L).build();

            given(userRepository.existsById(watcherId)).willReturn(true);
            given(watchingSessionRepository.findById(watcherId)).willReturn(Optional.of(session));
            given(userRepository.findById(watcherId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> watchingSessionService.getWatchingSession(watcherId))
                    .isInstanceOf(WatchingException.class)
                    .hasMessage(WatchingErrorCode.USER_NOT_FOUND.getMessage());
        }
    }
}