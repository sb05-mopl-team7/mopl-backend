package com.mopl.domain.review.controller.docs;

import com.mopl.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.domain.review.dto.response.CursorResponseReviewDto;
import com.mopl.domain.review.dto.response.ReviewDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "리뷰 관리", description = "리뷰 관련 API")
@RequestMapping("/api/reviews")
public interface ReviewControllerDocs {

    @Operation(
            summary = "리뷰 목록 조회 (최신순 커서 페이지네이션)",
            description = "contentId 기준 최신순 커서 페이지네이션 조회"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping
    ResponseEntity<CursorResponseReviewDto> findAll(
            @RequestParam Long contentId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String idAfter,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) Integer limit,

            @Parameter(description = "정렬 기준(", schema = @Schema(allowableValues = {"createdAt"}))
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,

            @Parameter(description = "정렬 방향", schema = @Schema(allowableValues = {"DESCENDING"}))
            @RequestParam(required = false, defaultValue = "DESCENDING") String sortDirection
    );

    @Operation(summary = "리뷰 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping
    ResponseEntity<ReviewDto> create(
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId,
            @Valid @RequestBody ReviewCreateRequest request
    );

    @Operation(summary = "리뷰 단건 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404", description = "리소스 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{reviewId}")
    ResponseEntity<ReviewDto> find(@PathVariable Long reviewId);

    @Operation(summary = "리뷰 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "403", description = "권한 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PatchMapping("/{reviewId}")
    ResponseEntity<ReviewDto> update(
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request
    );

    @Operation(summary = "리뷰 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "403", description = "권한 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{reviewId}")
    ResponseEntity<Void> delete(
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId,
            @PathVariable Long reviewId
    );
}
