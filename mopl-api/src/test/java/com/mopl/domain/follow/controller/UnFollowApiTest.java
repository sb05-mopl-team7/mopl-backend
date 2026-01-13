package com.mopl.domain.follow.controller;

import com.mopl.domain.follow.entity.Follow;
import com.mopl.domain.follow.exception.FollowErrorCode;
import com.mopl.domain.follow.repository.FollowRepository;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.user.enums.Role;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UnFollowApiTest {

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

    // 1. 성공 케이스 (204)
    @Test
    @DisplayName("[204] 본인이 생성한 팔로우를 취소하면 성공한다")
    void unfollowUser_Success() throws Exception {
        // Given: 팔로우 관계 미리 생성
        Follow follow = Follow.builder().follower(user1).followee(user2).build();
        Follow savedFollow = followRepository.save(follow);

        // When & Then
        mockMvc.perform(delete("/api/follows/{followId}", savedFollow.getId())
                        .with(authentication(createAuthToken(user1))) // 작성자(user1)로 로그인
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    // 2. 비즈니스 예외 케이스 (400)
    @Test
    @DisplayName("[400] 팔로우 ID가 숫자가 아닌 경우(TypeMismatch) BAD_REQUEST 에러 발생")
    void unfollowUser_TypeMismatch_Fail() throws Exception {
        // When: 숫자가 들어가야 할 자리에 문자("abc") 입력
        mockMvc.perform(delete("/api/follows/abc")
                        .with(authentication(createAuthToken(user1)))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value(ErrorCode.INVALID_INPUT_VALUE.name()));
    }

    // 3. 비즈니스 예외 케이스 (403)
    @Test
    @DisplayName("[403] 다른 사람의 팔로우를 취소하려 하면 FORBIDDEN 에러 발생")
    void unfollowUser_Forbidden_Fail() throws Exception {
        // Given: user1이 user2를 팔로우 함
        Follow follow = Follow.builder().follower(user1).followee(user2).build();
        Follow savedFollow = followRepository.save(follow);

        // When: user2가 user1의 팔로우를 삭제 시도
        mockMvc.perform(delete("/api/follows/{followId}", savedFollow.getId())
                        .with(authentication(createAuthToken(user2)))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value(FollowErrorCode.NOT_YOUR_FOLLOW.name()));
    }

    // 4. 리소스 없음 (404)
    @Test
    @DisplayName("[404] 존재하지 않는 팔로우 ID를 삭제하려 하면 NOT_FOUND 에러 발생")
    void unfollowUser_NotFound_Fail() throws Exception {
        // Given
        Long invalidFollowId = 99999L;

        // When & Then
        mockMvc.perform(delete("/api/follows/{followId}", invalidFollowId)
                        .with(authentication(createAuthToken(user1)))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value(FollowErrorCode.FOLLOW_NOT_FOUND.name()));
    }
}