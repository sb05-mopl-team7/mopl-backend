package com.mopl.domain.watching.controller.docs;

import com.mopl.domain.watching.dto.response.WatchingSessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "시청 세션 관리", description = "실시간 콘텐츠 시청 정보 API")
@RequestMapping("/api/users")
public interface WatchingSessionControllerDocs {

    @Operation(
            summary = "특정 사용자의 시청 세션 조회",
            description = "특정 사용자가 현재 실시간으로 시청 중인 콘텐츠 정보를 조회합니다. 시청 중인 정보가 없을 경우 null을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (시청 중이지 않을 경우 응답 바디가 비어있을 수 있음)"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (ID 형식 오류 등)"),
            @ApiResponse(responseCode = "401", description = "인증 오류 (로그인 필요)"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{watcherId}/watching-sessions")
    ResponseEntity<WatchingSessionResponse> getWatchingSession(
            @Parameter(description = "조회할 사용자 ID", required = true)
            @PathVariable Long watcherId
    );
}