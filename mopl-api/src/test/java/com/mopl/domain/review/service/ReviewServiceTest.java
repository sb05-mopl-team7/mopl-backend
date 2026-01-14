package com.mopl.domain.review.service;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.enums.ContentType;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.domain.review.dto.response.ReviewDto;
import com.mopl.domain.review.repository.ReviewRepository;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.exception.MoplException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ReviewServiceTest {

    @Autowired private ReviewService reviewService;
    @Autowired private UserRepository userRepository;
    @Autowired private ContentRepository contentRepository;
    @Autowired private ReviewRepository reviewRepository;

    private User user1;
    private User user2;
    private Content content;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(new User("me", "user1@test.com", "password"));
        user2 = userRepository.save(new User("you", "user2@test.com", "password"));

        content = contentRepository.save(new Content(
                ContentType.values()[0],
                "테스트 콘텐츠 제목",
                "테스트 콘텐츠 설명",
                "https://example.com/thumb.png"
        ));
    }

    private ReviewDto createReviewAs(Long userId, String text, double rating) {
        ReviewCreateRequest request = new ReviewCreateRequest(content.getId(), text, rating);
        return reviewService.create(userId, request);
    }

    @Test
    @DisplayName("[기능] 리뷰 생성 성공")
    void createReview_Success() {
        ReviewCreateRequest request = new ReviewCreateRequest(
                content.getId(),
                "재밌게 봤어요!",
                5.0
        );

        ReviewDto dto = reviewService.create(user1.getId(), request);

        assertNotNull(dto);
        assertNotNull(dto.id());
        assertEquals(content.getId(), dto.contentId());
        assertEquals("재밌게 봤어요!", dto.text());
        assertEquals(5.0, dto.rating());
    }

    @Test
    @DisplayName("[기능] 리뷰 생성 - 로그인 안됨(requesterId null) 이면 예외")
    void createReview_Unauthorized_Fail() {
        ReviewCreateRequest request = new ReviewCreateRequest(
                content.getId(),
                "내용",
                4.0
        );

        assertThrows(MoplException.class, () -> reviewService.create(null, request));
    }

    @Test
    @DisplayName("[기능] 리뷰 생성 - 존재하지 않는 contentId 이면 예외")
    void createReview_ContentNotFound_Fail() {
        ReviewCreateRequest request = new ReviewCreateRequest(
                999999L,
                "내용",
                3.0
        );

        assertThrows(MoplException.class, () -> reviewService.create(user1.getId(), request));
    }

    @Test
    @DisplayName("[기능] 목록 조회 - 최신 정렬(createdAt DESC) 아니면 예외")
    void findAll_InvalidSort_Fail() {
        assertThrows(MoplException.class, () ->
                reviewService.findAllLatestCursor(
                        content.getId(),
                        null,
                        null,
                        10,
                        "rating",      // createdAt만 허용
                        "ASCENDING"    // DESCENDING만 허용
                )
        );
    }

    // 수정(Update) 테스트 추가
    @Test
    @DisplayName("[기능] 리뷰 수정 성공 (작성자)")
    void updateReview_Success() {
        ReviewDto created = createReviewAs(user1.getId(), "원본", 3.0);

        ReviewUpdateRequest request = new ReviewUpdateRequest("수정됨", 4.5);
        ReviewDto updated = reviewService.update(user1.getId(), created.id(), request);

        assertNotNull(updated);
        assertEquals(created.id(), updated.id());
        assertEquals(created.contentId(), updated.contentId());
        assertEquals("수정됨", updated.text());
        assertEquals(4.5, updated.rating());
    }

    @Test
    @DisplayName("[기능] 리뷰 수정 - 로그인 안됨(requesterId null) 이면 예외")
    void updateReview_Unauthorized_Fail() {
        ReviewDto created = createReviewAs(user1.getId(), "원본", 3.0);

        ReviewUpdateRequest request = new ReviewUpdateRequest("수정", 4.0);
        assertThrows(MoplException.class, () -> reviewService.update(null, created.id(), request));
    }

    @Test
    @DisplayName("[기능] 리뷰 수정 - 작성자 아니면 예외")
    void updateReview_Forbidden_Fail() {
        ReviewDto created = createReviewAs(user1.getId(), "원본", 3.0);

        ReviewUpdateRequest request = new ReviewUpdateRequest("남이수정", 1.0);
        assertThrows(MoplException.class, () -> reviewService.update(user2.getId(), created.id(), request));
    }

    @Test
    @DisplayName("[기능] 리뷰 수정 - 존재하지 않는 reviewId 이면 예외")
    void updateReview_NotFound_Fail() {
        ReviewUpdateRequest request = new ReviewUpdateRequest("수정", 4.0);
        assertThrows(MoplException.class, () -> reviewService.update(user1.getId(), 999999L, request));
    }

    // 삭제(Delete) 테스트 추가

    @Test
    @DisplayName("[기능] 리뷰 삭제 성공 (작성자)")
    void deleteReview_Success() {
        ReviewDto created = createReviewAs(user1.getId(), "삭제될 리뷰", 2.0);

        reviewService.delete(user1.getId(), created.id());

        assertTrue(reviewRepository.findById(created.id()).isEmpty());
    }

    @Test
    @DisplayName("[기능] 리뷰 삭제 - 로그인 안됨(requesterId null) 이면 예외")
    void deleteReview_Unauthorized_Fail() {
        ReviewDto created = createReviewAs(user1.getId(), "삭제될 리뷰", 2.0);

        assertThrows(MoplException.class, () -> reviewService.delete(null, created.id()));
    }

    @Test
    @DisplayName("[기능] 리뷰 삭제 - 작성자 아니면 예외")
    void deleteReview_Forbidden_Fail() {
        ReviewDto created = createReviewAs(user1.getId(), "삭제될 리뷰", 2.0);

        assertThrows(MoplException.class, () -> reviewService.delete(user2.getId(), created.id()));
    }

    @Test
    @DisplayName("[기능] 리뷰 삭제 - 존재하지 않는 reviewId 이면 예외")
    void deleteReview_NotFound_Fail() {
        assertThrows(MoplException.class, () -> reviewService.delete(user1.getId(), 999999L));
    }
}