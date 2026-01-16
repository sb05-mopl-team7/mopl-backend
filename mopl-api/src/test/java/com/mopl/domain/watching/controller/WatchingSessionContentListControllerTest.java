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
import com.mopl.domain.watching.repository.WatchingSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private WatchingSessionRepository watchingSessionRepository;

    private User savedUser;
    private Content savedContent;

    @BeforeEach
    void setUp() {
        savedUser = userRepository.save(new User("테스터", "test@mopl.io", "password"));
        savedContent = contentRepository.save(new Content(ContentType.movie, "인터스텔라", "우주 영화", "url"));
    }

    @AfterEach
    void tearDown() {
        watchingSessionRepository.deleteAll();
    }

    private UsernamePasswordAuthenticationToken createAuthToken(User user) {
        UserPrincipal principal = new UserPrincipal(user.getId(), user.getEmail(), Role.USER);
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    // 1. 성공 케이스 (200)
    @Test
    @DisplayName("[200] 특정 콘텐츠의 시청 세션 목록을 성공적으로 조회한다")
    void getWatchingSessions_Success() throws Exception {
        // Given
        WatchingSession session = WatchingSession.builder()
                .id(savedUser.getId())
                .contentId(savedContent.getId())
                .createdAt(LocalDateTime.now())
                .build();
        watchingSessionRepository.save(session);

        // When & Then
        mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", savedContent.getId())
                        .param("limit", "10")
                        .param("sortDirection", "DESCENDING")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(authentication(createAuthToken(savedUser))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].watcher.name").value("테스터"))
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    @DisplayName("[200] 이름 필터링(watcherNameLike)을 적용하여 목록을 조회한다")
    void getWatchingSessions_Filtering_Success() throws Exception {
        // Given
        WatchingSession session = WatchingSession.builder()
                .id(savedUser.getId())
                .contentId(savedContent.getId())
                .build();
        watchingSessionRepository.save(session);

        // When & Then: 일치하는 이름 검색
        mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", savedContent.getId())
                        .param("watcherNameLike", "테스터")
                        .with(authentication(createAuthToken(savedUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1));

        // When & Then: 일치하지 않는 이름 검색 (빈 목록)
        mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", savedContent.getId())
                        .param("watcherNameLike", "없는사람")
                        .with(authentication(createAuthToken(savedUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // 2. 비즈니스 예외 케이스 (400)
    @Test
    @DisplayName("[400] 잘못된 페이지 limit(0 이하) 요청 시 INVALID_PAGINATION_LIMIT 에러 발생")
    void getWatchingSessions_InvalidLimit_Fail() throws Exception {
        mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", savedContent.getId())
                        .param("limit", "0")
                        .with(authentication(createAuthToken(savedUser))))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value(WatchingErrorCode.INVALID_PAGINATION_LIMIT.name()))
                .andExpect(jsonPath("$.detail").value(WatchingErrorCode.INVALID_PAGINATION_LIMIT.getMessage()));
    }

    @Test
    @DisplayName("[400] 잘못된 커서 날짜 형식 요청 시 INVALID_CURSOR 에러 발생")
    void getWatchingSessions_InvalidCursor_Fail() throws Exception {
        // Given: 세션이 하나라도 있어야 커서 파싱 로직을 타므로 데이터 추가
        watchingSessionRepository.save(new WatchingSession(savedUser.getId(), savedContent.getId(), LocalDateTime.now()));

        mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", savedContent.getId())
                        .param("cursor", "not-a-date-format")
                        .param("idAfter", "1")
                        .with(authentication(createAuthToken(savedUser))))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value(WatchingErrorCode.INVALID_CURSOR.name()));
    }

    // 3. 인증 예외 케이스 (401)
    @Test
    @DisplayName("[401] 인증되지 않은 사용자가 목록 조회 시 오류 발생")
    void getWatchingSessions_Unauthorized_Fail() throws Exception {
        mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", savedContent.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}