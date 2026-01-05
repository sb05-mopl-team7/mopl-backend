package com.mopl.domain.review.controller;

import com.mopl.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.domain.review.dto.response.ReviewDto;
import com.mopl.domain.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "리뷰 관리", description = "리뷰 관련 API")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    // 아직 커서 페이지네이션 미구현이라 contentId 기준 단순 조회만
    @Operation(
            summary = "리뷰 목록 조회",
            description = "리뷰 목록을 조회합니다. "
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping
    public ResponseEntity<List<ReviewDto>> findAll(
            @RequestParam Long contentId,

            @RequestParam(required = false)
            String cursor,

            @RequestParam(required = false)
            String idAfter,

            @RequestParam(required = false, defaultValue = "20")
            @Min(1) @Max(100)
            Integer limit,

            @Parameter(
                    description = "정렬 기준",
                    schema = @Schema(allowableValues = {"createdAt", "rating"})
            )
            @RequestParam(required = false, defaultValue = "createdAt")
            String sortBy,

            @Parameter(
                    description = "정렬 방향",
                    schema = @Schema(allowableValues = {"ASCENDING", "DESCENDING"})
            )
            @RequestParam(required = false, defaultValue = "DESCENDING")
            String sortDirection
    ) {
        // TODO 커서 페이지네이션 구현 시 cursor/idAfter/limit/sortBy/sortDirection 사용
        return ResponseEntity.ok(reviewService.findAllByContentId(contentId));
    }

    @Operation(
            summary = "리뷰 생성",
            description = "생성한 리뷰는 API 요청자 본인의 리뷰로 생성됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "201", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping
    public ResponseEntity<ReviewDto> create(
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        ReviewDto response = reviewService.create(requesterId, request);
        return ResponseEntity.status(201).body(response);
    }

    @Operation(
            summary = "리뷰 수정",
            description = "리뷰를 수정합니다. (리뷰 작성자만 수정할 수 있습니다.)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "403", description = "권한 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PatchMapping("/{reviewId}")
    public ResponseEntity<ReviewDto> update(
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request
    ) {
        return ResponseEntity.ok(reviewService.update(requesterId, reviewId, request));
    }

    @Operation(
            summary = "리뷰 삭제",
            description = "리뷰 작성자만 삭제할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 오류"),
            @ApiResponse(responseCode = "403", description = "권한 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId,
            @PathVariable Long reviewId
    ) {
        reviewService.delete(requesterId, reviewId);
        return ResponseEntity.noContent().build();
    }
}