package com.mopl.domain.watching.controller;

import com.mopl.domain.watching.dto.response.WatchingSessionUserResponse;
import com.mopl.domain.watching.exception.WatchingErrorCode;
import com.mopl.domain.watching.exception.WatchingException;
import com.mopl.domain.watching.service.WatchingSessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WatchingSessionController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("시청 세션 유저 컨트롤러 테스트")
class WatchingSessionUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WatchingSessionService watchingSessionService;

    @Nested
    @DisplayName("GET /api/users/{watcherId}/watching-sessions")
    class Describe_getWatchingSession {

        @Test
        @WithMockUser
        @DisplayName("[200] 특정 사용자의 시청 세션 정보를 성공적으로 조회한다")
        void it_returns_200_ok() throws Exception {
            Long watcherId = 123L;
            WatchingSessionUserResponse response = WatchingSessionUserResponse.builder()
                    .id(watcherId)
                    .createdAt(LocalDateTime.now())
                    .build();

            given(watchingSessionService.getWatchingSession(watcherId)).willReturn(response);

            mockMvc.perform(get("/api/users/{watcherId}/watching-sessions", watcherId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(watcherId.toString()));
        }

        @Test
        @WithMockUser
        @DisplayName("[204] 시청 중인 세션이 없을 경우 빈 바디를 반환한다")
        void it_returns_204_no_content() throws Exception {
            given(watchingSessionService.getWatchingSession(anyLong())).willReturn(null);

            mockMvc.perform(get("/api/users/{watcherId}/watching-sessions", 123L))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
        }

        @Test
        @WithMockUser
        @DisplayName("[400] 잘못된 요청 파라미터가 주어지면 에러를 반환한다")
        void it_returns_400_bad_request() throws Exception {
            given(watchingSessionService.getWatchingSession(-1L))
                    .willThrow(new WatchingException(WatchingErrorCode.INVALID_WATCHING_REQUEST));

            mockMvc.perform(get("/api/users/{watcherId}/watching-sessions", -1L))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("[404] 존재하지 않는 사용자 ID로 조회하면 에러를 반환한다")
        void it_returns_404_not_found() throws Exception {
            given(watchingSessionService.getWatchingSession(999L))
                    .willThrow(new WatchingException(WatchingErrorCode.USER_NOT_FOUND));

            mockMvc.perform(get("/api/users/{watcherId}/watching-sessions", 999L))
                    .andExpect(status().isNotFound());
        }
    }
}