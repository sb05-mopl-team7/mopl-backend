package com.mopl.domain.watching.controller;

import com.mopl.domain.watching.dto.response.WatchingSessionContentListResponse;
import com.mopl.domain.watching.dto.response.WatchingSessionUserResponse;
import com.mopl.domain.watching.exception.WatchingErrorCode;
import com.mopl.domain.watching.exception.WatchingException;
import com.mopl.domain.watching.service.WatchingSessionService;
import com.mopl.global.enums.SortDirection;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WatchingSessionController.class)
@AutoConfigureMockMvc(addFilters = false) // 인증 필터를 제외하여 컨트롤러 로직에 집중
@DisplayName("시청 세션 콘텐츠 리스트 컨트롤러 테스트")
class WatchingSessionContentListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WatchingSessionService watchingSessionService;

    @Nested
    @DisplayName("GET /api/contents/{contentId}/watching-sessions")
    class Describe_getWatchingSessionsByContent {

        @Test
        @WithMockUser
        @DisplayName("[200] 특정 콘텐츠의 시청 세션 목록을 성공적으로 조회한다")
        void it_returns_200_ok_with_list() throws Exception {
            // given
            Long contentId = 456L;
            LocalDateTime now = LocalDateTime.now();

            WatchingSessionUserResponse userResponse = WatchingSessionUserResponse.builder()
                    .id(123L)
                    .createdAt(now)
                    .build();

            WatchingSessionContentListResponse listResponse = WatchingSessionContentListResponse.builder()
                    .data(List.of(userResponse))
                    .nextCursor(now.toString())
                    .nextIdAfter(123L)
                    .hasNext(true)
                    .totalCount(100L)
                    .sortBy("createdAt")
                    .sortDirection(SortDirection.DESCENDING)
                    .build();

            given(watchingSessionService.getWatchingSessionsByContent(
                    eq(contentId), anyString(), any(), any(), anyInt(), anyString(), any(SortDirection.class)
            )).willReturn(listResponse);

            // when & then
            mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", contentId)
                            .param("watcherNameLike", "우디")
                            .param("limit", "10")
                            .param("sortBy", "createdAt")
                            .param("sortDirection", "DESCENDING"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.totalCount").value(100))
                    .andExpect(jsonPath("$.data[0].id").value("123"))
                    .andExpect(jsonPath("$.nextIdAfter").value("123"));
        }

        @Test
        @WithMockUser
        @DisplayName("[200] 시청자가 없을 경우 빈 목록과 함께 성공 응답을 반환한다")
        void it_returns_200_ok_with_empty_list() throws Exception {
            // given
            Long contentId = 456L;
            WatchingSessionContentListResponse emptyResponse = WatchingSessionContentListResponse.empty("createdAt", SortDirection.DESCENDING);

            given(watchingSessionService.getWatchingSessionsByContent(
                    eq(contentId), any(), any(), any(), anyInt(), anyString(), any(SortDirection.class)
            )).willReturn(emptyResponse);

            // when & then
            mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", contentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andExpect(jsonPath("$.totalCount").value(0));
        }

        @Test
        @WithMockUser
        @DisplayName("[400] 잘못된 정렬 기준이나 파라미터가 주어지면 에러를 반환한다")
        void it_returns_400_bad_request() throws Exception {
            // given
            Long contentId = 456L;
            given(watchingSessionService.getWatchingSessionsByContent(
                    eq(contentId), any(), any(), any(), anyInt(), anyString(), any(SortDirection.class)
            )).willThrow(new WatchingException(WatchingErrorCode.INVALID_WATCHING_REQUEST));

            // when & then
            mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", contentId)
                            .param("sortBy", "invalidField")) // 잘못된 필드로 정렬 요청 상황 시뮬레이션
                    .andExpect(status().isBadRequest());
        }

        // [참고] 401 테스트는 Security Filter 설정이 포함되어야 하므로
        // 컨트롤러 단위 테스트인 본 파일에서는 제외하거나 Filter를 켜고 별도 설정을 해야 합니다.
    }
}