package com.mopl.domain.watching.service;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.domain.watching.dto.response.WatchingSessionContentListResponse;
import com.mopl.domain.watching.entity.WatchingSession;
import com.mopl.domain.watching.repository.WatchingSessionRepository;
import com.mopl.global.enums.SortDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("시청 세션 목록 조회 서비스 테스트")
class WatchingSessionContentListServiceTest {

    @InjectMocks
    private WatchingSessionService watchingSessionService;

    @Mock
    private WatchingSessionRepository watchingSessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContentRepository contentRepository;

    @Nested
    @DisplayName("콘텐츠별 시청 목록 조회")
    class Describe_getWatchingSessionsByContent {

        @Test
        @DisplayName("[성공] 최신순 정렬 및 리밋에 따른 페이징 목록을 반환한다")
        void it_returns_paged_list_with_default_sort() {
            // given
            Long contentId = 100L;
            LocalDateTime now = LocalDateTime.now();

            WatchingSession s1 = WatchingSession.builder().id(1L).contentId(contentId).createdAt(now.minusMinutes(10)).build();
            WatchingSession s2 = WatchingSession.builder().id(2L).contentId(contentId).createdAt(now.minusMinutes(5)).build();
            WatchingSession s3 = WatchingSession.builder().id(3L).contentId(contentId).createdAt(now).build();

            given(watchingSessionRepository.findAllByContentId(contentId)).willReturn(List.of(s1, s2, s3));
            given(contentRepository.findByIdWithTags(contentId)).willReturn(Optional.of(mock(Content.class)));

            User u1 = mock(User.class); given(u1.getId()).willReturn(1L); given(u1.getName()).willReturn("user1");
            User u2 = mock(User.class); given(u2.getId()).willReturn(2L); given(u2.getName()).willReturn("user2");
            User u3 = mock(User.class); given(u3.getId()).willReturn(3L); given(u3.getName()).willReturn("user3");

            given(userRepository.findAllById(anyList())).willReturn(List.of(u1, u2, u3));

            // when
            WatchingSessionContentListResponse response = watchingSessionService.getWatchingSessionsByContent(
                    contentId, null, null, null, 2, "createdAt", SortDirection.DESCENDING
            );

            // then
            assertThat(response.data()).hasSize(2);
            assertThat(response.totalCount()).isEqualTo(3);
            assertThat(response.hasNext()).isTrue();
            assertThat(response.data().get(0).id()).isEqualTo(3L);
        }

        @Test
        @DisplayName("[필터] 사용자 이름 검색 조건에 맞는 결과만 필터링한다")
        void it_filters_by_watcher_name() {
            // given
            Long contentId = 100L;
            WatchingSession s1 = WatchingSession.builder().id(1L).contentId(contentId).build();

            given(watchingSessionRepository.findAllByContentId(contentId)).willReturn(List.of(s1));
            given(contentRepository.findByIdWithTags(contentId)).willReturn(Optional.of(mock(Content.class)));

            User u1 = mock(User.class);
            given(u1.getId()).willReturn(1L);
            given(u1.getName()).willReturn("우디");
            given(userRepository.findAllById(anyList())).willReturn(List.of(u1));

            // when
            var response = watchingSessionService.getWatchingSessionsByContent(
                    contentId, "버즈", null, null, 10, "createdAt", SortDirection.DESCENDING
            );

            // then
            assertThat(response.data()).isEmpty();
            assertThat(response.totalCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("[페이징] 커서 이후의 데이터부터 정확히 조절하여 반환한다")
        void it_starts_from_after_cursor() {
            // given
            Long contentId = 100L;
            LocalDateTime time1 = LocalDateTime.of(2026, 1, 19, 10, 0);
            LocalDateTime time2 = LocalDateTime.of(2026, 1, 19, 11, 0);

            WatchingSession s1 = WatchingSession.builder().id(1L).contentId(contentId).createdAt(time1).build();
            WatchingSession s2 = WatchingSession.builder().id(2L).contentId(contentId).createdAt(time2).build();

            given(watchingSessionRepository.findAllByContentId(contentId)).willReturn(List.of(s1, s2));
            given(contentRepository.findByIdWithTags(contentId)).willReturn(Optional.of(mock(Content.class)));

            User u1 = mock(User.class); given(u1.getId()).willReturn(1L); given(u1.getName()).willReturn("u1");
            User u2 = mock(User.class); given(u2.getId()).willReturn(2L); given(u2.getName()).willReturn("u2");
            given(userRepository.findAllById(anyList())).willReturn(List.of(u1, u2));

            // when
            var response = watchingSessionService.getWatchingSessionsByContent(
                    contentId, null, time2.toString(), 2L, 10, "createdAt", SortDirection.DESCENDING
            );

            // then
            assertThat(response.data()).hasSize(1);
            assertThat(response.data().get(0).id()).isEqualTo(1L);
            assertThat(response.hasNext()).isFalse();
        }
    }
}