package com.mopl.domain.review.controller.docs;

import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.domain.review.dto.response.ReviewDto;
import com.mopl.global.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "리뷰 관리", description = "리뷰 관련 API")
public interface ReviewControllerDocs {

    @Operation(summary = "리뷰 목록 조회 (커서 페이지네이션, 최신순만 지원)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<PageResponse<ReviewDto>> findAllLatestCursor(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "콘텐츠 ID") Long contentId,
            @Parameter(description = "커서(createdAt)") String cursor,
            @Parameter(description = "보조 커서(id)") String idAfter,
            @Parameter(description = "한 번에 가져올 개수") Integer limit,
            @Parameter(description = "정렬 기준(고정: createdAt)") String sortBy,
            @Parameter(description = "정렬 방향(고정: DESCENDING)") String sortDirection
    );

    @Operation(summary = "리뷰 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<ReviewDto> create(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ReviewCreateRequest request
    );

    @Operation(summary = "리뷰 단건 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "404", description = "대상 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<ReviewDto> find(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "리뷰 ID") Long reviewId
    );

    @Operation(summary = "리뷰 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "403", description = "권한 없음(작성자 아님)"),
            @ApiResponse(responseCode = "404", description = "대상 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<ReviewDto> update(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "리뷰 ID") Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request
    );

    @Operation(summary = "리뷰 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "403", description = "권한 없음(작성자 아님)"),
            @ApiResponse(responseCode = "404", description = "대상 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<Void> delete(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "리뷰 ID") Long reviewId
    );
}