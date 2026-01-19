package com.mopl.domain.watching.controller;

import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.enums.ContentType;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.enums.Role;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.domain.watching.entity.WatchingSession;
import com.mopl.domain.watching.exception.WatchingErrorCode;
import com.mopl.global.redis.RedisManager;
import com.mopl.global.redis.RedisNameSpace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WatchingSessionContentListControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ContentRepository contentRepository;
    @Autowired private RedisManager redisManager;

    private User savedUser;
    private Content savedContent;

    @BeforeEach
    void setUp() {
        savedUser = userRepository.save(new User("테스터", "test@mopl.io", "password"));
        savedContent = contentRepository.save(new Content(ContentType.movie, "인터스텔라", "우주 영화", "url"));
    }

    @AfterEach
    void tearDown() {
        redisManager.delete(RedisNameSpace.USER_WATCHING, String.valueOf(savedUser.getId()));
        redisManager.removeFromSet(RedisNameSpace.CONTENT_WATCHERS, String.valueOf(savedContent.getId()), savedUser.getId());
    }

    private UsernamePasswordAuthenticationToken createAuthToken(User user) {
        UserPrincipal principal = new UserPrincipal(user.getId(), user.getEmail(), Role.USER);
        return new UsernamePasswordAuthenticationToken(principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

    // --- [SUCCESS CASES] ---

    @Test
    @DisplayName("[200] 특정 콘텐츠의 시청 세션 목록을 성공적으로 조회한다")
    void getWatchingSessionsByContent_Success() throws Exception {
        // Given
        Long userId = savedUser.getId();
        Long contentId = savedContent.getId();

        WatchingSession session = WatchingSession.builder()
                .id(userId).contentId(contentId).createdAt(LocalDateTime.now()).build();

        redisManager.saveHash(RedisNameSpace.USER_WATCHING, String.valueOf(userId), session);
        redisManager.addToSet(RedisNameSpace.CONTENT_WATCHERS, String.valueOf(contentId), userId);

        // When & Then
        mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", contentId)
                        .param("limit", "10")
                        .param("sortBy", "createdAt")
                        .param("sortDirection", "DESCENDING")
                        .with(authentication(createAuthToken(savedUser))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.data[0].watcher.name").value("테스터"))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("[200] 이름 필터링(watcherNameLike)을 적용하여 목록을 조회한다")
    void getWatchingSessions_Filtering_Success() throws Exception {
        // Given
        redisManager.saveHash(RedisNameSpace.USER_WATCHING, String.valueOf(savedUser.getId()),
                WatchingSession.builder().id(savedUser.getId()).contentId(savedContent.getId()).createdAt(LocalDateTime.now()).build());
        redisManager.addToSet(RedisNameSpace.CONTENT_WATCHERS, String.valueOf(savedContent.getId()), savedUser.getId());

        // 일치하는 이름 검색
        mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", savedContent.getId())
                        .param("watcherNameLike", "테스터")
                        .with(authentication(createAuthToken(savedUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1));

        // 일치하지 않는 이름 검색
        mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", savedContent.getId())
                        .param("watcherNameLike", "없는사람")
                        .with(authentication(createAuthToken(savedUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    // --- [ERROR & SECURITY CASES] ---

    @Test
    @DisplayName("[400] 잘못된 커서 날짜 형식 요청 시 INVALID_CURSOR 에러 발생")
    void getWatchingSessions_InvalidCursor_Fail() throws Exception {
        mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", savedContent.getId())
                        .param("cursor", "invalid-date-format")
                        .param("idAfter", String.valueOf(savedUser.getId()))
                        .with(authentication(createAuthToken(savedUser))))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(WatchingErrorCode.INVALID_CURSOR.getErrorCode()));
    }

    @Test
    @DisplayName("[400] 허용되지 않은 limit 값(0 이하 또는 100 초과) 요청 시 에러 발생")
    void getWatchingSessions_InvalidLimit_Fail() throws Exception {
        mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", savedContent.getId())
                        .param("limit", "0")
                        .with(authentication(createAuthToken(savedUser))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(WatchingErrorCode.INVALID_PAGINATION_LIMIT.getErrorCode()));
    }

    @Test
    @DisplayName("[401] 인증되지 않은 사용자 접근 시 401 Unauthorized 발생")
    void getWatchingSessions_Unauthorized_Fail() throws Exception {
        mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", savedContent.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("[404] 존재하지 않는 콘텐츠 ID로 조회 시 CONTENT_NOT_FOUND 에러 발생")
    void getWatchingSessions_ContentNotFound_Fail() throws Exception {
        mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", 9999L)
                        .with(authentication(createAuthToken(savedUser))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(WatchingErrorCode.CONTENT_NOT_FOUND.getErrorCode()));
    }
}