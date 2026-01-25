package com.mopl.domain.watching.controller.docs;

import com.mopl.domain.watching.dto.response.WatchingSessionContentListResponse;
import com.mopl.domain.watching.dto.response.WatchingSessionUserResponse;
import com.mopl.global.enums.SortDirection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "시청 세션 관리", description = "실시간 콘텐츠 시청 정보 API")
@RequestMapping("/api")
public interface WatchingSessionControllerDocs {

    @Operation(
            summary = "특정 사용자의 시청 세션 조회",
            description = "특정 사용자가 현재 실시간으로 시청 중인 콘텐츠 정보를 조회합니다. 시청 중이지 않을 경우 응답 바디가 비어있을 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "204", description = "시청 중인 세션 없음"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/users/{watcherId}/watching-sessions")
    ResponseEntity<WatchingSessionUserResponse> getWatchingSession(
            @Parameter(description = "조회할 사용자 ID (Long)", required = true, example = "123")
            @PathVariable Long watcherId
    );

    @Operation(
            summary = "특정 콘텐츠의 시청 세션 목록 조회",
            description = "특정 콘텐츠를 현재 시청 중인 사용자 목록을 커서 기반 페이지네이션으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "401", description = "인증 오류")
    })
    @GetMapping("/contents/{contentId}/watching-sessions")
    ResponseEntity<WatchingSessionContentListResponse> getWatchingSessionsByContent(
            @Parameter(description = "콘텐츠 ID (Long)", required = true, example = "456")
            @PathVariable Long contentId,

            @Parameter(description = "시청자 이름 검색 (부분 일치)")
            @RequestParam(required = false) String watcherNameLike,

            @Parameter(description = "페이지네이션 커서 (전페이지 마지막 데이터의 createdAt, ISO-8601 형식)", example = "2026-01-19T06:46:27.803")
            @RequestParam(required = false) String cursor,

            @Parameter(description = "보조 커서 (전페이지 마지막 데이터의 watcherId)", example = "123")
            @RequestParam(required = false) Long idAfter,

            @Parameter(description = "조회 개수", required = true, example = "10")
            @RequestParam(defaultValue = "10") Integer limit,

            @Parameter(description = "정렬 기준 (현재 createdAt만 지원)")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "정렬 방향", required = true)
            @RequestParam(defaultValue = "DESCENDING") SortDirection sortDirection
    );
}