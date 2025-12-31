package com.mopl.domain.review.repository;

import com.mopl.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    long countByContentId(Long contentId);
}
