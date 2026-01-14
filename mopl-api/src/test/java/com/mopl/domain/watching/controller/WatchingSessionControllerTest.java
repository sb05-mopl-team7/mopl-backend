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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc; // 인지 완료
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
class WatchingSessionControllerTest {

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
        Content content = new Content(ContentType.movie, "인터스텔라", "우주 영화", "https://image.com/thumb.jpg");
        savedContent = contentRepository.save(content);
    }

    @AfterEach
    void tearDown() {
        watchingSessionRepository.deleteAll();
    }

    // 인증 토큰 생성 헬퍼 메서드 (SecurityConfig의 인증 요구사항 충족)
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
    @DisplayName("[200] 시청 중인 세션 조회 성공 시 상세 정보를 반환한다")
    void getWatchingSession_Success() throws Exception {
        // Given
        WatchingSession session = WatchingSession.builder()
                .id(savedUser.getId())
                .contentId(savedContent.getId())
                .build();
        watchingSessionRepository.save(session);

        // When & Then
        mockMvc.perform(get("/api/users/{watcherId}/watching-sessions", savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(authentication(createAuthToken(savedUser)))) // 인증 정보 추가
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(String.valueOf(savedUser.getId())))
                .andExpect(jsonPath("$.watcher.name").value("테스터"))
                .andExpect(jsonPath("$.content.title").value("인터스텔라"));
    }

    @Test
    @DisplayName("[200] 시청 중인 정보가 없으면 Null을 반환한다 (정상 케이스)")
    void getWatchingSession_Null_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users/{watcherId}/watching-sessions", savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(authentication(createAuthToken(savedUser))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").doesNotExist());
    }

    // 2. 비즈니스 예외 케이스 (400)
    @Test
    @DisplayName("[400] 잘못된 요청 파라미터(음수 ID) 시 INVALID_WATCHING_REQUEST 에러 발생")
    void getWatchingSession_InvalidRequest_Fail() throws Exception {
        mockMvc.perform(get("/api/users/{watcherId}/watching-sessions", -1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(authentication(createAuthToken(savedUser))))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value(WatchingErrorCode.INVALID_WATCHING_REQUEST.name()))
                .andExpect(jsonPath("$.detail").value(WatchingErrorCode.INVALID_WATCHING_REQUEST.getMessage()));
    }

    // 3. 리소스 없음 케이스 (404)
    @Test
    @DisplayName("[404] 존재하지 않는 유저 조회 시 USER_NOT_FOUND 에러 발생")
    void getWatchingSession_UserNotFound_Fail() throws Exception {
        mockMvc.perform(get("/api/users/{watcherId}/watching-sessions", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(authentication(createAuthToken(savedUser))))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value(WatchingErrorCode.USER_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(WatchingErrorCode.USER_NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("[404] 세션에 등록된 콘텐츠가 DB에 없으면 CONTENT_NOT_FOUND 에러 발생")
    void getWatchingSession_ContentNotFound_Fail() throws Exception {
        WatchingSession session = WatchingSession.builder()
                .id(savedUser.getId())
                .contentId(888888L)
                .build();
        watchingSessionRepository.save(session);

        mockMvc.perform(get("/api/users/{watcherId}/watching-sessions", savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(authentication(createAuthToken(savedUser))))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value(WatchingErrorCode.CONTENT_NOT_FOUND.name()))
                .andExpect(jsonPath("$.detail").value(WatchingErrorCode.CONTENT_NOT_FOUND.getMessage()));
    }
}