package com.mopl.domain.review.repository;

import com.mopl.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    long countByContentId(Long contentId);

    List<Review> findAllByContentIdOrderByCreatedAtDesc(Long contentId);
}
