package com.mopl.domain.review.repository;

import com.mopl.domain.review.entity.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 최신순(createdAt DESC) 커서 페이지네이션
    @Query("""
        select r
        from Review r
        where r.contentId = :contentId
          and (
            :cursorCreatedAt is null
            or r.createdAt < :cursorCreatedAt
            or (r.createdAt = :cursorCreatedAt and r.id < :idAfter)
          )
        order by r.createdAt desc, r.id desc
    """)
    List<Review> cursorLatest(
            @Param("contentId") Long contentId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("idAfter") Long idAfter,
            Pageable pageable
    );


    Optional<Review> findByIdAndUserId(Long id, Long userId);
}
