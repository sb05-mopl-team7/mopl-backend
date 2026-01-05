package com.mopl.domain.review.service;

import com.mopl.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.domain.review.dto.response.ReviewAuthorDto;
import com.mopl.domain.review.dto.response.ReviewDto;
import com.mopl.domain.review.entity.Review;
import com.mopl.domain.review.repository.ReviewRepository;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

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

    private void validateAuthenticated(Long requesterId) {
        if (requesterId == null) {
            throw new MoplException(ErrorCode.UNAUTHORIZED);
        }
    }

    private ReviewDto toDto(Review review) {
        // User 도메인 연동 전이라서 userId만 채우고 나머지는 null
        ReviewAuthorDto author = new ReviewAuthorDto(review.getUserId(), null, null);

        return new ReviewDto(
                review.getId(),
                review.getContentId(),
                author,
                review.getText(),
                review.getRating()
        );
    }
}
