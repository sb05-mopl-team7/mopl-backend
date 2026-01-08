package com.mopl.domain.playlist.controller.docs;

import com.mopl.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.domain.playlist.dto.response.PlaylistDto;
import com.mopl.global.dto.PageResponse;
import com.mopl.global.enums.SortDirection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.security.Principal;

@Tag(name = "플레이리스트 관리", description = "플레이리스트 관련 API")
public interface PlaylistControllerDocs {

    // 1) GET /api/playlists
    @Operation(summary = "플레이리스트 목록 조회 (커서 페이지네이션)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<PageResponse<PlaylistDto>> findAll(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "검색 키워드") String keywordLike,
            @Parameter(description = "소유자 ID") Long ownerIdEqual,
            @Parameter(description = "구독자 ID") Long subscriberIdEqual,
            @Parameter(description = "커서 키") String cursor,
            @Parameter(description = "보조 커서") String idAfter,
            @Parameter(description = "한 번에 가져올 개수") Integer limit,
            @Schema(allowableValues = {"ASCENDING", "DESCENDING"}) SortDirection sortDirection,
            @Schema(allowableValues = {"updatedAt", "subscribeCount"}) String sortBy
    );

    // 2) POST /api/playlists
    @Operation(summary = "플레이리스트 생성", description = "생성한 플레이리스트는 API 요청자 본인의 플레이리스트로 생성됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "201", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<PlaylistDto> create(
            @Parameter(hidden = true) Principal principal,
            @RequestBody PlaylistCreateRequest request
    );

    // 3) POST /api/playlists/{playlistId}/subscription
    @Operation(summary = "플레이리스트 구독")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "204", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<Void> subscribe(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "playlistId") Long playlistId
    );

    // 4) DELETE /api/playlists/{playlistId}/subscription
    @Operation(summary = "플레이리스트 구독 취소")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "204", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<Void> unsubscribe(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "playlistId") Long playlistId
    );

    // 5) POST /api/playlists/{playlistId}/contents/{contentId}
    @Operation(summary = "플레이리스트에 콘텐츠 추가", description = "플레이리스트 소유자만 콘텐츠를 추가할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "204", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "403", description = "권한 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<Void> addContent(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "playlistId") Long playlistId,
            @Parameter(description = "contentId") Long contentId
    );

    // 6) DELETE /api/playlists/{playlistId}/contents/{contentId}
    @Operation(summary = "플레이리스트에서 콘텐츠 삭제", description = "플레이리스트 소유자만 콘텐츠를 삭제할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "204", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "403", description = "권한 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<Void> removeContent(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "playlistId") Long playlistId,
            @Parameter(description = "contentId") Long contentId
    );

    // 7) GET /api/playlists/{playlistId}
    @Operation(summary = "플레이리스트 단건 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<PlaylistDto> find(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "playlistId") Long playlistId
    );

    // 8) DELETE /api/playlists/{playlistId}
    @Operation(summary = "플레이리스트 삭제", description = "플레이리스트 소유자만 삭제할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "403", description = "권한 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<Void> delete(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "playlistId") Long playlistId
    );

    // 9) PATCH /api/playlists/{playlistId}
    @Operation(summary = "플레이리스트 수정", description = "플레이리스트 소유자만 수정할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "403", description = "권한 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<PlaylistDto> update(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "playlistId") Long playlistId,
            @RequestBody PlaylistUpdateRequest request
    );
}