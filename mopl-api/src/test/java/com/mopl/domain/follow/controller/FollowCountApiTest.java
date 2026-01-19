package com.mopl.domain.follow.controller;

import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.follow.entity.Follow;
import com.mopl.domain.follow.repository.FollowRepository;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.enums.Role;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.exception.ErrorCode;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FollowCountApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FollowRepository followRepository;

    // 테스트용 유저
    private User targetUser;
    private User fan1, fan2;

    @BeforeEach
    void setUp() {
        // 유저 생성
        targetUser = userRepository.save(new User("star", "star@test.com", "password"));
        fan1 = userRepository.save(new User("fan1", "fan1@test.com", "password"));
        fan2 = userRepository.save(new User("fan2", "fan2@test.com", "password"));
    }

    // UserPrincipal을 포함한 인증 토큰 생성 헬퍼 메서드
    private UsernamePasswordAuthenticationToken createAuthToken(User user) {

        // 실제 컨트롤러가 사용하는 UserPrincipal 객체 생성
        UserPrincipal principal = new UserPrincipal(user.getId(), user.getEmail(), Role.USER);

        // SecurityContext에 들어갈 토큰 생성
        return new UsernamePasswordAuthenticationToken(
                principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    // 1. 성공 케이스 - 데이터 있음 (200)
    @Test
    @DisplayName("[200] 팔로워가 있는 경우 정확한 카운트 숫자를 반환한다")
    void getFollowCounts_Success() throws Exception {
        // Given
        // targetUser를 2명이 팔로우 (팔로워 = 2)
        followRepository.save(Follow.builder().follower(fan1).followee(targetUser).build());
        followRepository.save(Follow.builder().follower(fan2).followee(targetUser).build());

        // When & Then
        mockMvc.perform(get("/api/follows/count")
                        .param("followeeId", String.valueOf(targetUser.getId())) // targetId -> followeeId로 수정
                        .with(authentication(createAuthToken(fan1))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }

    // 2. 성공 케이스 - 데이터 없음 (200)
    @Test
    @DisplayName("[200] 팔로워가 없으면 0을 반환한다")
    void getFollowCounts_Zero() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/follows/count")
                        .param("followeeId", String.valueOf(targetUser.getId())) // targetId -> followeeId로 수정
                        .with(authentication(createAuthToken(fan1))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    // 3. 비즈니스 예외 케이스 (400)
    @Test
    @DisplayName("[400] 필수 파라미터(followeeId) 누락 시 에러 발생")
    void getFollowCounts_MissingParam() throws Exception {
        // When & Then (followeeId 파라미터 없이 요청)
        mockMvc.perform(get("/api/follows/count")
                        .with(authentication(createAuthToken(fan1))))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value(ErrorCode.MISSING_INPUT_VALUE.name()));
    }

    // 4. 인증 예외 케이스 (401)
    @Test
    @DisplayName("[401] 로그인하지 않고 요청하면 인증 에러 발생")
    void getFollowCounts_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/follows/count")
                        .param("followeeId", String.valueOf(targetUser.getId()))) // targetId -> followeeId로 수정
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    // 5. 비즈니스 예외 케이스 (404)
    @Test
    @DisplayName("[404] 존재하지 않는 유저를 조회하면 NOT_FOUND 에러 발생")
    void getFollowCounts_NotFound() throws Exception {
        // Given
        long nonExistentId = 999999L;

        // When & Then
        mockMvc.perform(get("/api/follows/count")
                        .param("followeeId", String.valueOf(nonExistentId)) // targetId -> followeeId로 수정
                        .with(authentication(createAuthToken(fan1))))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value(ErrorCode.NOT_FOUND.name()));
    }
}