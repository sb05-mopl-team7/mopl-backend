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
import com.mopl.global.enums.SortDirection;
import com.mopl.global.redis.RedisManager;
import com.mopl.global.redis.RedisNameSpace;
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
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WatchingSessionContentListServiceTest {

    @InjectMocks
    private WatchingSessionService watchingSessionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private RedisManager redisManager;

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
            WatchingSession s1 = WatchingSession.builder().id(1L).contentId(contentId).createdAt(now.minusMinutes(10)).build();
            WatchingSession s2 = WatchingSession.builder().id(2L).contentId(contentId).createdAt(now.minusMinutes(5)).build();
            WatchingSession s3 = WatchingSession.builder().id(3L).contentId(contentId).createdAt(now).build();

            // RedisManager Mock 로직: Set에서 ID 목록을 가져오고, Hash에서 각 상세 정보를 가져온다
            given(redisManager.getSetMembers(eq(RedisNameSpace.CONTENT_WATCHERS), anyString(), eq(Long.class)))
                    .willReturn(Set.of(1L, 2L, 3L));

            given(redisManager.findHashByKey(eq(RedisNameSpace.USER_WATCHING), eq("1"), eq(WatchingSession.class))).willReturn(Optional.of(s1));
            given(redisManager.findHashByKey(eq(RedisNameSpace.USER_WATCHING), eq("2"), eq(WatchingSession.class))).willReturn(Optional.of(s2));
            given(redisManager.findHashByKey(eq(RedisNameSpace.USER_WATCHING), eq("3"), eq(WatchingSession.class))).willReturn(Optional.of(s3));

            given(userRepository.findAllById(any())).willReturn(List.of(user1, user2, user3));
            given(contentRepository.findById(contentId)).willReturn(Optional.of(content));

            // When
            WatchingSessionContentListResponse response = watchingSessionService.getWatchingSessionsByContent(
                    contentId, null, null, null, 2, "createdAt", SortDirection.DESCENDING
            );

            // Then
            assertThat(response.data()).hasSize(2);
            assertThat(response.totalCount()).isEqualTo(3);
            assertThat(response.data().get(0).id()).isEqualTo("3");
        }

        @Test
        @DisplayName("[성공] 커서와 보조 커서를 사용하면 다음 데이터부터 반환한다")
        void success_CursorPagination() {
            // Given
            Long contentId = 100L;
            LocalDateTime time1 = LocalDateTime.of(2026, 1, 14, 10, 0);
            LocalDateTime time2 = LocalDateTime.of(2026, 1, 14, 11, 0);

            WatchingSession s1 = WatchingSession.builder().id(1L).contentId(contentId).createdAt(time1).build();
            WatchingSession s2 = WatchingSession.builder().id(2L).contentId(contentId).createdAt(time2).build();

            given(redisManager.getSetMembers(any(), anyString(), any())).willReturn(Set.of(1L, 2L));
            given(redisManager.findHashByKey(any(), eq("1"), any())).willReturn(Optional.of(s1));
            given(redisManager.findHashByKey(any(), eq("2"), any())).willReturn(Optional.of(s2));

            given(userRepository.findAllById(any())).willReturn(List.of(createUser(1L, "UserA"), createUser(2L, "UserB")));
            given(contentRepository.findById(contentId)).willReturn(Optional.of(createContent(contentId)));

            // When: s1을 커서로 넘겼을 때 오름차순 조회
            WatchingSessionContentListResponse response = watchingSessionService.getWatchingSessionsByContent(
                    contentId, null, time1.toString(), 1L, 10, "createdAt", SortDirection.ASCENDING
            );

            // Then
            assertThat(response.data()).hasSize(1);
            assertThat(response.data().get(0).id()).isEqualTo("2");
        }

        @Test
        @DisplayName("[성공] 시청 중인 세션이 전혀 없으면 빈 목록을 반환한다")
        void success_EmptyList() {

            // Given Set이 비어있는 상황 Mocking
            Long contentId = 100L;

            given(redisManager.getSetMembers(any(), anyString(), any())).willReturn(Set.of());

            // When
            WatchingSessionContentListResponse response = watchingSessionService.getWatchingSessionsByContent(
                    contentId, null, null, null, 10, "createdAt", SortDirection.DESCENDING
            );

            // Then
            assertThat(response.data()).isEmpty();
            assertThat(response.totalCount()).isZero();
        }

        @Test
        @DisplayName("[실패] 커서 형식이 잘못된 경우 INVALID_CURSOR 예외가 발생한다")
        void fail_InvalidCursor() {
            // Given
            Long contentId = 100L;
            WatchingSession s1 = WatchingSession.builder().id(1L).contentId(contentId).createdAt(LocalDateTime.now()).build();

            given(redisManager.getSetMembers(any(), anyString(), any())).willReturn(Set.of(1L));
            given(redisManager.findHashByKey(any(), anyString(), any())).willReturn(Optional.of(s1));
            given(userRepository.findAllById(any())).willReturn(List.of(createUser(1L, "테스터")));
            given(contentRepository.findById(anyLong())).willReturn(Optional.of(createContent(contentId)));

            // When & Then
            assertThatThrownBy(() -> watchingSessionService.getWatchingSessionsByContent(
                    contentId, null, "invalid-date-format", 1L, 10, "createdAt", SortDirection.DESCENDING))
                    .isInstanceOf(WatchingException.class)
                    .hasMessage(WatchingErrorCode.INVALID_CURSOR.getMessage());
        }
    }
}