package com.mopl.domain.follow.controller;

import com.mopl.domain.follow.exception.FollowErrorCode;
import tools.jackson.databind.ObjectMapper;
import com.mopl.domain.follow.dto.request.FollowRequest;
import com.mopl.domain.follow.entity.Follow;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FollowApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // 테스트용 유저
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        // 유저 생성
        user1 = userRepository.save(new User("me", "user1@test.com", "password"));
        user2 = userRepository.save(new User("you", "user2@test.com", "password"));
    }

    // 1. 성공 케이스 (201)
    @Test
    @DisplayName("[201] 정상적인 팔로우 요청 시 성공한다")
    void followUser_Success() throws Exception {
        // Given
        FollowRequest request = new FollowRequest(user2.getId());

        // When & Then
        mockMvc.perform(post("/api/follows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(String.valueOf(user1.getId())))) // 로그인
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.followerId").value(user1.getId()))
                .andExpect(jsonPath("$.followeeId").value(user2.getId()));
    }

    // 2. 비즈니스 예외 케이스 (400)
    @Test
    @DisplayName("[400] 자기 자신을 팔로우하면 CANNOT_FOLLOW_SELF 에러 발생")
    void followUser_Self_Fail() throws Exception {
        // Given
        FollowRequest request = new FollowRequest(user1.getId());

        // When & Then
        mockMvc.perform(post("/api/follows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(String.valueOf(user1.getId()))))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value(FollowErrorCode.CANNOT_FOLLOW_SELF.name()))
                .andExpect(jsonPath("$.detail").value(FollowErrorCode.CANNOT_FOLLOW_SELF.getMessage()));
    }

    @Test
    @DisplayName("[400] 이미 팔로우 중인데 또 요청하면 ALREADY_FOLLOWING 에러 발생")
    void followUser_Duplicate_Fail() throws Exception {
        // Given
        followRepository.save(Follow.builder().follower(user1).followee(user2).build());

        FollowRequest request = new FollowRequest(user2.getId());

        // When & Then
        mockMvc.perform(post("/api/follows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(String.valueOf(user1.getId()))))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value(FollowErrorCode.ALREADY_FOLLOWING.name()))
                .andExpect(jsonPath("$.detail").value(FollowErrorCode.ALREADY_FOLLOWING.getMessage()));
    }

    // 3. 인증 실패 (401)
    @Test
    @DisplayName("[401] 로그인 정보 없이 요청하면 UNAUTHORIZED 에러 발생")
    void followUser_Unauthorized_Fail() throws Exception {
        // Given
        FollowRequest request = new FollowRequest(user2.getId());

        // When & Then
        mockMvc.perform(post("/api/follows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value(ErrorCode.UNAUTHORIZED.name()));
    }

    // 4. 리소스 없음 (404)
    @Test
    @DisplayName("[404] 존재하지 않는 유저를 팔로우하면 NOT_FOUND 에러 발생")
    void followUser_NotFound_Fail() throws Exception {

        // Given
        Long nonExistentId = 999999L;
        FollowRequest request = new FollowRequest(nonExistentId);

        // When & Then
        mockMvc.perform(post("/api/follows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user(String.valueOf(user1.getId()))))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value(ErrorCode.NOT_FOUND.name()));
    }

}