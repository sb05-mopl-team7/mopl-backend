package com.mopl.domain.playlist.controller.docs;

import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.domain.playlist.dto.request.PlaylistSearchCondition;
import com.mopl.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.domain.playlist.dto.response.PlaylistDto;
import com.mopl.global.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "플레이리스트 관리", description = "플레이리스트 관련 API")
public interface PlaylistControllerDocs {

    @Operation(summary = "플레이리스트 목록 조회 (커서 페이지네이션)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<PageResponse<PlaylistDto>> findAll(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @ModelAttribute @ParameterObject PlaylistSearchCondition condition
    );

    @Operation(summary = "플레이리스트 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<PlaylistDto> create(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody PlaylistCreateRequest request
    );

    @Operation(summary = "플레이리스트 구독")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<Void> subscribe(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "playlistId") @PathVariable Long playlistId
    );

    @Operation(summary = "플레이리스트 구독 취소")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<Void> unsubscribe(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "playlistId") @PathVariable Long playlistId
    );

    @Operation(summary = "플레이리스트에 콘텐츠 추가")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "403", description = "권한 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<Void> addContent(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "playlistId") @PathVariable Long playlistId,
            @Parameter(description = "contentId") @PathVariable Long contentId
    );

    @Operation(summary = "플레이리스트에서 콘텐츠 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "403", description = "권한 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<Void> removeContent(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "playlistId") @PathVariable Long playlistId,
            @Parameter(description = "contentId") @PathVariable Long contentId
    );

    @Operation(summary = "플레이리스트 단건 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<PlaylistDto> find(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "playlistId") @PathVariable Long playlistId
    );

    @Operation(summary = "플레이리스트 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "403", description = "권한 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<Void> delete(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "playlistId") @PathVariable Long playlistId
    );

    @Operation(summary = "플레이리스트 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "403", description = "권한 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<PlaylistDto> update(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "playlistId") @PathVariable Long playlistId,
            @Valid @RequestBody PlaylistUpdateRequest request
    );
}
