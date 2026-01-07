package com.mopl.domain.review.controller;

import com.mopl.domain.review.controller.docs.ReviewControllerDocs;
import com.mopl.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.domain.review.dto.response.ReviewDto;
import com.mopl.domain.review.service.ReviewService;
import com.mopl.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReviewController implements ReviewControllerDocs {

    private final ReviewService reviewService;

    @Override
    public ResponseEntity<PageResponse<ReviewDto>> findAll(
            Long contentId,
            String cursor,
            String idAfter,
            Integer limit,
            String sortBy,
            String sortDirection
    ) {
        return ResponseEntity.ok(
                reviewService.findAllLatestCursor(contentId, cursor, idAfter, limit, sortBy, sortDirection)
        );
    }

    @Override
    public ResponseEntity<ReviewDto> create(Long requesterId, ReviewCreateRequest request) {
        return ResponseEntity.status(201).body(reviewService.create(requesterId, request));
    }

    @Override
    public ResponseEntity<ReviewDto> find(Long reviewId) {
        return ResponseEntity.ok(reviewService.find(reviewId));
    }

    @Override
    public ResponseEntity<ReviewDto> update(Long requesterId, Long reviewId, ReviewUpdateRequest request) {
        return ResponseEntity.ok(reviewService.update(requesterId, reviewId, request));
    }

    @Override
    public ResponseEntity<Void> delete(Long requesterId, Long reviewId) {
        reviewService.delete(requesterId, reviewId);
        return ResponseEntity.noContent().build();
    }
}