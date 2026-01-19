package com.mopl.domain.content.repository;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.enums.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ContentRepository extends JpaRepository<Content, Long>, ContentRepositoryCustom {

    @Query("""
        select c
        from Content c
        left join fetch c.contentTags ct
        left join fetch ct.tag t
        where c.id = :id
    """)
    Optional<Content> findByIdWithTags(@Param("id") Long id);

    //playlist 조회 시 tags까지 한 번에 끌고 오기 위한 fetch join (N+1 방지)
    @Query("""
        select distinct c
        from Content c
        left join fetch c.contentTags ct
        left join fetch ct.tag t
        where c.id in :ids
    """)
    List<Content> findAllByIdInWithTags(@Param("ids") Collection<Long> ids);

    /**
     * 리뷰 등록 시 사용할 원자적 커밋
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Content c
        SET c.averageRating =
                ((c.averageRating * c.reviewCount) + :rating)
                / (c.reviewCount + 1),
            c.reviewCount = c.reviewCount + 1
        WHERE c.id = :contentId
    """)
    void increaseReview(
            @Param("contentId") Long contentId,
            @Param("rating") double rating
    );

    /**
    * 리뷰 업데이트 시 사용할 원자적 커밋
    */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Content c
        SET c.averageRating = CASE
                WHEN c.reviewCount = 0 THEN 0.0
                ELSE ((c.averageRating * c.reviewCount) - :oldRating + :newRating)
                     / c.reviewCount
            END
        WHERE c.id = :contentId
    """)
    void updateReviewRating(
            @Param("contentId") Long contentId,
            @Param("oldRating") double oldRating,
            @Param("newRating") double newRating
    );

    /**
     * 리뷰 삭제 시 사용할 원자적 커밋
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Content c
        SET c.averageRating = CASE
                WHEN c.reviewCount <= 1 THEN 0.0
                ELSE ((c.averageRating * c.reviewCount) - :rating)
                     / (c.reviewCount - 1)
            END,
            c.reviewCount = c.reviewCount - 1
        WHERE c.id = :contentId
    """)
    void decreaseReview(
            @Param("contentId") Long contentId,
            @Param("rating") double rating
    );

    @Query("SELECT c.originId FROM Content c WHERE c.originId IN :originIds AND c.contentType = :type")
    Set<Long> findExistingOriginIds(@Param("originIds") List<Long> originIds, @Param("type") ContentType type);

    boolean existsByOriginIdAndContentType(Long originId, ContentType type);
}