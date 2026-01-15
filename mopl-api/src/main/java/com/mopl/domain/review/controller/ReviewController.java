package com.mopl.domain.review.controller;

import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.review.controller.docs.ReviewControllerDocs;
import com.mopl.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.domain.review.dto.response.ReviewDto;
import com.mopl.domain.review.service.ReviewService;
import com.mopl.global.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController implements ReviewControllerDocs {

    private final ReviewService reviewService;

    @Override
    @GetMapping
    public ResponseEntity<PageResponse<ReviewDto>> findAllLatestCursor(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam Long contentId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String idAfter,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection
    ) {
        return ResponseEntity.ok(
                reviewService.findAllLatestCursor(contentId, cursor, idAfter, limit, sortBy, sortDirection)
        );
    }

    @Override
    @PostMapping
    public ResponseEntity<ReviewDto> create(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        long requesterId = userPrincipal.getUserId();
        ReviewDto created = reviewService.create(requesterId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewDto> find(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long reviewId
    ) {
        return ResponseEntity.ok(reviewService.find(reviewId));
    }

    @Override
    @PatchMapping("/{reviewId}")
    public ResponseEntity<ReviewDto> update(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request
    ) {
        long requesterId = userPrincipal.getUserId();
        return ResponseEntity.ok(reviewService.update(requesterId, reviewId, request));
    }

    @Override
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long reviewId
    ) {
        Long userId = userPrincipal.getUserId();
        reviewService.delete(userId, reviewId);
        return ResponseEntity.ok().build();
    }
}