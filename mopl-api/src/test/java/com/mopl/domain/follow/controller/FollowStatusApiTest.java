package com.mopl.domain.follow.controller;

import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.follow.entity.Follow;
import com.mopl.domain.follow.repository.FollowRepository;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.enums.Role;
import com.mopl.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FollowStatusApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FollowRepository followRepository;

    // 테스트용 유저
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        // 유저 생성
        user1 = userRepository.save(new User("me", "user1@test.com", "password"));
        user2 = userRepository.save(new User("you", "user2@test.com", "password"));
    }

    // UserPrincipal을 포함한 인증 토큰 생성 헬퍼 메서드
    private UsernamePasswordAuthenticationToken createAuthToken(User user) {

        // 실제 컨트롤러가 사용하는 UserPrincipal 객체 생성
        UserPrincipal principal = new UserPrincipal(user.getId(), user.getEmail(), Role.USER);

        // SecurityContext에 들어갈 토큰 생성
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("[200] 팔로우 중일 경우 true를 반환한다")
    void checkFollowStatus_True() throws Exception {
        // Given: user1 -> user2 팔로우 관계 생성
        followRepository.save(Follow.builder().follower(user1).followee(user2).build());

        // When & Then: user1이 로그인한 상태로 user2 팔로우 여부 조회
        mockMvc.perform(get("/api/follows/followed-by-me")
                        .param("followeeId", String.valueOf(user2.getId()))
                        .with(authentication(createAuthToken(user1))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("[200] 팔로우 중이 아닐 경우 false를 반환한다")
    void checkFollowStatus_False() throws Exception {
        // Given: 팔로우 관계 없음

        // When & Then: user1이 로그인한 상태로 user2 팔로우 여부 조회
        mockMvc.perform(get("/api/follows/followed-by-me")
                        .param("followeeId", String.valueOf(user2.getId()))
                        .with(authentication(createAuthToken(user1))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("[200] 존재하지 않는 유저 ID로 조회해도 false를 반환한다")
    void checkFollowStatus_NotFoundUser_False() throws Exception {
        // Given: 존재하지 않는 ID
        long nonExistentUserId = 999999L;

        // When & Then
        mockMvc.perform(get("/api/follows/followed-by-me")
                        .param("followeeId", String.valueOf(nonExistentUserId))
                        .with(authentication(createAuthToken(user1))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("[401] 로그인하지 않고 조회하면 UNAUTHORIZED 에러 발생")
    void checkFollowStatus_Unauthorized() throws Exception {
        // Given
        Long targetId = user2.getId();

        // When & Then: .with(authentication(...)) 생략
        mockMvc.perform(get("/api/follows/followed-by-me")
                        .param("followeeId", String.valueOf(targetId)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("[400] 필수 파라미터(followeeId)가 없으면 BAD_REQUEST 발생")
    void checkFollowStatus_NoParam_Fail() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/follows/followed-by-me")
                        .with(authentication(createAuthToken(user1))))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}