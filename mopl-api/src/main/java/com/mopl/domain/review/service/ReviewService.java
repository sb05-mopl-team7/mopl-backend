package com.mopl.domain.review.service;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.exception.ContentErrorCode;
import com.mopl.domain.content.exception.ContentException;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.domain.review.dto.response.ReviewAuthorDto;
import com.mopl.domain.review.dto.response.ReviewDto;
import com.mopl.domain.review.entity.Review;
import com.mopl.domain.review.exception.ReviewErrorCode;
import com.mopl.domain.review.exception.ReviewException;
import com.mopl.domain.review.repository.ReviewRepository;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.exception.UserErrorCode;
import com.mopl.domain.user.exception.UserException;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.dto.PageResponse;
import com.mopl.global.enums.SortDirection;
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
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;

    @Transactional
    public ReviewDto create(Long userId, ReviewCreateRequest request) {

        User user = validateUser(userId);
        Content content = validateContentExists(request.contentId());

        Review review = new Review(
                user.getId(),
                content.getId(),
                request.text(),
                request.rating()
        );

        Review saved = reviewRepository.save(review);
        reviewRepository.refreshReviewStats(content.getId());

        ReviewAuthorDto author = loadAuthor(saved.getUserId());
        return toDto(saved, author);
    }

    @Transactional
    public ReviewDto update(Long userId, Long reviewId, ReviewUpdateRequest request) {

        Review review = getReviewByUser(userId, reviewId);
        review.update(request.text(), request.rating());

        Content content = validateContentExists(review.getContentId());
        reviewRepository.refreshReviewStats(content.getId());

        ReviewAuthorDto author = loadAuthor(review.getUserId());
        return toDto(review, author);
    }

    @Transactional
    public void delete(Long userId, Long reviewId) {
        Review review = getReviewByUser(userId, reviewId);
        if(!userId.equals(review.getUserId())){
            throw new ReviewException(ReviewErrorCode.NOT_REVIEW_OWNER);
        }
        reviewRepository.delete(review);

        Content content = validateContentExists(review.getContentId());
        reviewRepository.refreshReviewStats(content.getId());

    }

    @Transactional(readOnly = true)
    public ReviewDto find(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new MoplException(ErrorCode.NOT_FOUND));

        ReviewAuthorDto author = loadAuthor(review.getUserId());
        return toDto(review, author);
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
        validateContentExists(contentId);
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

        // N+1 방지: 페이지에 있는 userId만 모아서 한 번에 조회
        Set<Long> userIds = new HashSet<>();
        for (Review r : page) userIds.add(r.getUserId());

        Map<Long, ReviewAuthorDto> authorMap = loadAuthorMap(userIds);

        List<ReviewDto> data = page.stream()
                .map(r -> {
                    ReviewAuthorDto author = authorMap.getOrDefault(
                            r.getUserId(),
                            new ReviewAuthorDto(r.getUserId(), null, null)
                    );
                    return toDto(r, author);
                })
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




    /** 사용자 검증 */
    private User validateUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_EXIST));
    }

    /** 콘텐츠 검증 */
    private Content validateContentExists(Long contentId) {
        return contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.CONTENT_NOT_FOUND));
    }

    private Review validateReviewExists(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new MoplException(ErrorCode.NOT_FOUND));
    }

    /** 사용자가 작성한 리뷰 조회 */
    private Review getReviewByUser(Long userId, Long reviewId) {
        validateUser(userId);
        return reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new MoplException(ErrorCode.NOT_FOUND));
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

    // 커서 정규화(중복 페이지 방지): DB createdAt 정밀도(마이크로초)에 맞춤
    private LocalDateTime normalizeCursorTime(LocalDateTime t) {
        return t == null ? null : t.truncatedTo(ChronoUnit.MICROS);
    }

    private LocalDateTime parseCreatedAtCursor(String raw) {
        String normalized = raw.trim().replace(" ", "T");
        try {
            LocalDateTime parsed = LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return normalizeCursorTime(parsed);
        } catch (DateTimeParseException e) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }
    }

    private String formatCreatedAtCursor(LocalDateTime createdAt) {
        return normalizeCursorTime(createdAt).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private Map<Long, ReviewAuthorDto> loadAuthorMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();

        List<User> users = userRepository.findAllById(userIds);

        Map<Long, ReviewAuthorDto> map = new HashMap<>();
        for (User u : users) {
            map.put(u.getId(), new ReviewAuthorDto(u.getId(), u.getName(), u.getProfileImageUrl()));
        }
        return map;
    }

    /** 작성자 정보를 ReviewAuthorDto 로 변환 */
    private ReviewAuthorDto loadAuthor(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_EXIST));
        return new ReviewAuthorDto(user.getId(), user.getName(), user.getProfileImageUrl());
    }

    private ReviewDto toDto(Review review, ReviewAuthorDto author) {
        return new ReviewDto(
                review.getId(),
                review.getContentId(),
                author,
                review.getText(),
                review.getRating()
        );
    }

    private record CursorKey(LocalDateTime cursorCreatedAt, Long idAfter) {}
}