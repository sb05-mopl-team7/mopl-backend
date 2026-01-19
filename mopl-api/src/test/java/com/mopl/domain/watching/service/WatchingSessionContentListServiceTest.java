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
        Content content = new Content(ContentType.movie, "인터스텔라", "우주 영화", "url");
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
            given(contentRepository.findById(contentId)).willReturn(Optional.of(createContent(contentId)));
            given(redisManager.getSetMembers(any(), anyString(), any())).willReturn(Set.of(1L));
            given(redisManager.findHashByKey(any(), eq("1"), any())).willReturn(Optional.of(WatchingSession.builder().id(1L).createdAt(LocalDateTime.now()).build()));
            given(userRepository.findAllById(any())).willReturn(List.of(createUser(1L, "테스터")));

            // When
            WatchingSessionContentListResponse response = watchingSessionService.getWatchingSessionsByContent(contentId, null, null, null, 10, "createdAt", SortDirection.DESCENDING);

            // Then
            assertThat(response.totalCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 콘텐츠 ID 조회 시 CONTENT_NOT_FOUND 예외가 발생한다")
        void fail_ContentNotFound() {
            given(contentRepository.findById(anyLong())).willReturn(Optional.empty());
            assertThatThrownBy(() -> watchingSessionService.getWatchingSessionsByContent(999L, null, null, null, 10, "createdAt", SortDirection.DESCENDING))
                    .isInstanceOf(WatchingException.class)
                    .hasMessage(WatchingErrorCode.CONTENT_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("[실패] 커서 형식이 잘못된 경우 INVALID_CURSOR 예외가 발생한다")
        void fail_InvalidCursor() {
            Long contentId = 100L;
            given(contentRepository.findById(contentId)).willReturn(Optional.of(createContent(contentId)));
            given(redisManager.getSetMembers(any(), anyString(), any())).willReturn(Set.of(1L));
            given(redisManager.findHashByKey(any(), anyString(), any())).willReturn(Optional.of(WatchingSession.builder().id(1L).createdAt(LocalDateTime.now()).build()));
            given(userRepository.findAllById(any())).willReturn(List.of(createUser(1L, "테스터")));

            assertThatThrownBy(() -> watchingSessionService.getWatchingSessionsByContent(contentId, null, "invalid", 1L, 10, "createdAt", SortDirection.DESCENDING))
                    .isInstanceOf(WatchingException.class)
                    .hasMessage(WatchingErrorCode.INVALID_CURSOR.getMessage());
        }
    }
}