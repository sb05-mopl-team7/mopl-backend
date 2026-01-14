package com.mopl.domain.watching.service;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.enums.ContentType;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.domain.watching.dto.response.WatchingSessionContentListResponse;
import com.mopl.domain.watching.entity.WatchingSession;
import com.mopl.domain.watching.exception.WatchingErrorCode;
import com.mopl.domain.watching.exception.WatchingException;
import com.mopl.domain.watching.repository.WatchingSessionRepository;
import com.mopl.global.enums.SortDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WatchingSessionContentListServiceTest {

    @InjectMocks
    private WatchingSessionService watchingSessionService;

    @Mock
    private WatchingSessionRepository watchingSessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContentRepository contentRepository;

    // 테스트 데이터 생성 헬퍼 메서드 유지
    private User createUser(Long id, String name) {
        User user = new User(name, name + "@test.com", "password");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Content createContent(Long id) {
        Content content = new Content(ContentType.movie, "인터스텔라", "우주 영화", "https://image.com/thumb.jpg");
        ReflectionTestUtils.setField(content, "id", id);
        return content;
    }

    @Nested
    @DisplayName("특정 콘텐츠 시청 세션 목록 조회 (getWatchingSessionsByContent)")
    class GetWatchingSessionsByContentTest {

        @Test
        @DisplayName("[성공] 정렬 및 페이징 조건에 맞는 세션 목록을 반환한다 (최신순)")
        void success_PagingAndSorting() {
            // Given
            Long contentId = 100L;
            Content content = createContent(contentId);

            User user1 = createUser(1L, "테스터1");
            User user2 = createUser(2L, "테스터2");
            User user3 = createUser(3L, "테스터3");

            LocalDateTime now = LocalDateTime.of(2026, 1, 14, 15, 0);
            WatchingSession s1 = new WatchingSession(1L, contentId, now.minusMinutes(10));
            WatchingSession s2 = new WatchingSession(2L, contentId, now.minusMinutes(5));
            WatchingSession s3 = new WatchingSession(3L, contentId, now);

            given(watchingSessionRepository.findAllByContentId(contentId)).willReturn(List.of(s1, s2, s3));
            given(userRepository.findAllById(anyList())).willReturn(List.of(user1, user2, user3));
            given(contentRepository.findAllById(anyList())).willReturn(List.of(content));

            // When: 내림차순(DESCENDING), limit 2로 조회
            WatchingSessionContentListResponse response = watchingSessionService.getWatchingSessionsByContent(
                    contentId, null, null, null, 2, "createdAt", SortDirection.DESCENDING
            );

            // Then
            assertThat(response.data()).hasSize(2);
            assertThat(response.hasNext()).isTrue();
            assertThat(response.totalCount()).isEqualTo(3);
            assertThat(response.data().get(0).id()).isEqualTo("3"); // 가장 최근 데이터
            assertThat(response.nextIdAfter()).isEqualTo("2"); // 다음 커서 ID
        }

        @Test
        @DisplayName("[성공] 이름 필터링(watcherNameLike) 조건이 포함된 목록만 반환한다")
        void success_NameFiltering() {
            // Given
            Long contentId = 100L;
            User user1 = createUser(1L, "김철수");
            User user2 = createUser(2L, "이영희");
            WatchingSession s1 = new WatchingSession(1L, contentId, LocalDateTime.now());
            WatchingSession s2 = new WatchingSession(2L, contentId, LocalDateTime.now());

            given(watchingSessionRepository.findAllByContentId(contentId)).willReturn(List.of(s1, s2));
            given(userRepository.findAllById(anyList())).willReturn(List.of(user1, user2));
            given(contentRepository.findAllById(anyList())).willReturn(List.of(createContent(contentId)));

            // When: "철수" 필터 적용
            WatchingSessionContentListResponse response = watchingSessionService.getWatchingSessionsByContent(
                    contentId, "철수", null, null, 10, "createdAt", SortDirection.DESCENDING
            );

            // Then
            assertThat(response.data()).hasSize(1);
            assertThat(response.data().get(0).watcher().name()).isEqualTo("김철수");
            assertThat(response.totalCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("[성공] 커서와 보조 커서를 사용하면 다음 데이터부터 반환한다")
        void success_CursorPagination() {
            // Given
            Long contentId = 100L;
            LocalDateTime time1 = LocalDateTime.of(2026, 1, 14, 10, 0);
            LocalDateTime time2 = LocalDateTime.of(2026, 1, 14, 11, 0);

            WatchingSession s1 = new WatchingSession(1L, contentId, time1);
            WatchingSession s2 = new WatchingSession(2L, contentId, time2);

            given(watchingSessionRepository.findAllByContentId(contentId)).willReturn(List.of(s1, s2));
            given(userRepository.findAllById(anyList())).willReturn(List.of(createUser(1L, "UserA"), createUser(2L, "UserB")));
            given(contentRepository.findAllById(anyList())).willReturn(List.of(createContent(contentId)));

            // When: s1(10시)을 커서로 넘겼을 때 오름차순 조회
            WatchingSessionContentListResponse response = watchingSessionService.getWatchingSessionsByContent(
                    contentId, null, time1.toString(), 1L, 10, "createdAt", SortDirection.ASCENDING
            );

            // Then
            assertThat(response.data()).hasSize(1);
            assertThat(response.data().get(0).id()).isEqualTo("2"); // 10시 다음인 11시 데이터
            assertThat(response.hasNext()).isFalse();
        }

        @Test
        @DisplayName("[성공] 시청 중인 세션이 전혀 없으면 빈 목록을 반환한다")
        void success_EmptyList() {
            // Given
            Long contentId = 100L;
            given(watchingSessionRepository.findAllByContentId(contentId)).willReturn(List.of());

            // When
            WatchingSessionContentListResponse response = watchingSessionService.getWatchingSessionsByContent(
                    contentId, null, null, null, 10, "createdAt", SortDirection.DESCENDING
            );

            // Then
            assertThat(response.data()).isEmpty();
            assertThat(response.totalCount()).isZero();
            assertThat(response.hasNext()).isFalse();
        }

        @Test
        @DisplayName("[실패] limit이 0 이하인 경우 INVALID_PAGINATION_LIMIT 예외가 발생한다")
        void fail_InvalidLimit() {
            // When & Then
            assertThatThrownBy(() -> watchingSessionService.getWatchingSessionsByContent(
                    1L, null, null, null, 0, "createdAt", SortDirection.DESCENDING))
                    .isInstanceOf(WatchingException.class)
                    .hasMessage(WatchingErrorCode.INVALID_PAGINATION_LIMIT.getMessage());
        }

        @Test
        @DisplayName("[실패] 커서 형식이 잘못된 경우 INVALID_CURSOR 예외가 발생한다")
        void fail_InvalidCursor() {
            // Given
            Long contentId = 100L;
            given(watchingSessionRepository.findAllByContentId(contentId)).willReturn(List.of(new WatchingSession(1L, 100L, LocalDateTime.now())));

            // When & Then
            assertThatThrownBy(() -> watchingSessionService.getWatchingSessionsByContent(
                    contentId, null, "invalid-date-format", 1L, 10, "createdAt", SortDirection.DESCENDING))
                    .isInstanceOf(WatchingException.class)
                    .hasMessage(WatchingErrorCode.INVALID_CURSOR.getMessage());
        }
    }
}