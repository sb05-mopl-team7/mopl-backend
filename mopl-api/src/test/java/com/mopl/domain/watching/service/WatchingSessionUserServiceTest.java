package com.mopl.domain.watching.service;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.domain.watching.dto.response.WatchingSessionUserResponse;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("시청 세션 유저 단건 조회 서비스 테스트")
class WatchingSessionUserServiceTest {

    @InjectMocks
    private WatchingSessionService watchingSessionService;

    @Mock
    private WatchingSessionRepository watchingSessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContentRepository contentRepository;

    @Nested
    @DisplayName("유저별 시청 세션 단건 조회")
    class Describe_getWatchingSession {

        @Test
        @DisplayName("[성공] 세션 정보가 존재하면 시청자 및 콘텐츠 정보를 결합하여 반환한다")
        void it_returns_combined_response() {
            // given
            Long watcherId = 1L;
            Long contentId = 100L;

            WatchingSession session = WatchingSession.builder()
                    .id(watcherId)
                    .contentId(contentId)
                    .build();

            User user = mock(User.class);
            given(user.getId()).willReturn(watcherId);
            given(user.getName()).willReturn("정건진");

            Content content = mock(Content.class);
            given(content.getId()).willReturn(contentId);

            given(watchingSessionRepository.findById(watcherId)).willReturn(Optional.of(session));
            given(userRepository.findById(watcherId)).willReturn(Optional.of(user));
            given(contentRepository.findByIdWithTags(contentId)).willReturn(Optional.of(content));

            // when
            WatchingSessionUserResponse response = watchingSessionService.getWatchingSession(watcherId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(watcherId);
            assertThat(response.watcher().name()).isEqualTo("정건진");
        }

        @Test
        @DisplayName("[성공] 시청 중인 세션이 존재하지 않으면 null을 반환한다")
        void it_returns_null() {
            // given
            given(watchingSessionRepository.findById(anyLong())).willReturn(Optional.empty());

            // when
            WatchingSessionUserResponse response = watchingSessionService.getWatchingSession(1L);

            // then
            assertThat(response).isNull();
        }

        @Test
        @DisplayName("[예외] 세션은 있으나 유저 정보가 DB에 없으면 USER_NOT_FOUND를 던진다")
        void it_throws_user_not_found_exception() {
            // given
            Long watcherId = 1L;
            WatchingSession session = WatchingSession.builder().id(watcherId).build();

            given(watchingSessionRepository.findById(watcherId)).willReturn(Optional.of(session));
            given(userRepository.findById(watcherId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> watchingSessionService.getWatchingSession(watcherId))
                    .isInstanceOf(WatchingException.class)
                    .hasMessage(WatchingErrorCode.USER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("[예외] 유효하지 않은 ID(음수 등)가 입력되면 INVALID_REQUEST를 던진다")
        void it_throws_invalid_request_exception() {
            // when & then
            assertThatThrownBy(() -> watchingSessionService.getWatchingSession(-1L))
                    .isInstanceOf(WatchingException.class)
                    .hasMessage(WatchingErrorCode.INVALID_WATCHING_REQUEST.getMessage());
        }
    }
}