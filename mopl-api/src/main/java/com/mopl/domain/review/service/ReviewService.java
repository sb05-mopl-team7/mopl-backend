package com.mopl.domain.review.service;

import com.mopl.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.domain.review.dto.response.ReviewAuthorDto;
import com.mopl.domain.review.dto.response.ReviewDto;
import com.mopl.domain.review.entity.Review;
import com.mopl.domain.review.repository.ReviewRepository;
import com.mopl.global.SortDirection;
import com.mopl.global.dto.PageResponse;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final ReviewRepository reviewRepository;

    @Transactional
    public ReviewDto create(Long requesterId, ReviewCreateRequest request) {
        validateAuthenticated(requesterId);

        Review review = new Review(
                requesterId,
                request.contentId(),
                request.text(),
                request.rating()
        );

        Review saved = reviewRepository.save(review);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public ReviewDto find(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new MoplException(ErrorCode.NOT_FOUND));
        return toDto(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> findAllLatestCursor(
            Long contentId,
            String cursor,
            String idAfter,
            Integer limit,
            String sortBy,
            String sortDirection
    ) {
        validateOnlyLatestSort(sortBy, sortDirection);

        int size = normalizeLimit(limit);
        CursorKey key = parseCursorKey(cursor, idAfter);

        Pageable pageable = PageRequest.of(0, size + 1);

        List<Review> fetched = reviewRepository.cursorLatest(
                contentId,
                key.cursorCreatedAt,
                key.idAfter,
                pageable
        );

        boolean hasNext = fetched.size() > size;
        List<Review> page = hasNext ? fetched.subList(0, size) : fetched;

        List<ReviewDto> data = page.stream()
                .map(this::toDto)
                .toList();

        String nextCursor = null;
        Long nextIdAfter = null;

        if (hasNext && !page.isEmpty()) {
            Review last = page.get(page.size() - 1);
            nextIdAfter = last.getId();
            nextCursor = formatCreatedAtCursor(last.getCreatedAt());
        }

        return PageResponse.<ReviewDto>builder()
                .data(data)
                .nextCursor(nextCursor)
                .nextIdAfter(nextIdAfter)
                .hasNext(hasNext)
                .totalCount(0L)
                .sortBy("createdAt")
                .sortDirection(SortDirection.DESCENDING)
                .build();
    }

    @Transactional
    public ReviewDto update(Long requesterId, Long reviewId, ReviewUpdateRequest request) {
        validateAuthenticated(requesterId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new MoplException(ErrorCode.NOT_FOUND));

        if (!review.isAuthor(requesterId)) {
            throw new MoplException(ErrorCode.FORBIDDEN);
        }

        review.update(request.text(), request.rating());
        return toDto(review);
    }

    @Transactional
    public void delete(Long requesterId, Long reviewId) {
        validateAuthenticated(requesterId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new MoplException(ErrorCode.NOT_FOUND));

        if (!review.isAuthor(requesterId)) {
            throw new MoplException(ErrorCode.FORBIDDEN);
        }

        reviewRepository.delete(review);
    }

    private void validateAuthenticated(Long requesterId) {
        if (requesterId == null) {
            throw new MoplException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void validateOnlyLatestSort(String sortBy, String sortDirection) {
        if (sortBy != null && !sortBy.isBlank() && !"createdAt".equalsIgnoreCase(sortBy.trim())) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }
        if (sortDirection != null && !sortDirection.isBlank() && !"DESCENDING".equalsIgnoreCase(sortDirection.trim())) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) return DEFAULT_LIMIT;
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }
        return limit;
    }

    private CursorKey parseCursorKey(String cursorRaw, String idAfterRaw) {
        boolean hasCursor = cursorRaw != null && !cursorRaw.isBlank();
        boolean hasIdAfter = idAfterRaw != null && !idAfterRaw.isBlank();

        if (hasCursor != hasIdAfter) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }

        if (!hasCursor) {
            return new CursorKey(null, null);
        }

        return new CursorKey(parseCreatedAtCursor(cursorRaw), parseLong(idAfterRaw));
    }

    private Long parseLong(String raw) {
        try {
            return Long.parseLong(raw.trim());
        } catch (Exception e) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }
    }

    private LocalDateTime parseCreatedAtCursor(String raw) {
        String normalized = raw.trim().replace(" ", "T");
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }
    }

    private String formatCreatedAtCursor(LocalDateTime createdAt) {
        return createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private ReviewDto toDto(Review review) {
        ReviewAuthorDto author = new ReviewAuthorDto(review.getUserId(), null, null);

        return new ReviewDto(
                review.getId(),
                review.getContentId(),
                author,
                review.getText(),
                review.getRating()
        );
    }

    private record CursorKey(LocalDateTime cursorCreatedAt, Long idAfter) {
    }
}