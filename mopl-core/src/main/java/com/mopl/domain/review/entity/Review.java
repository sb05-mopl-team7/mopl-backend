package com.mopl.domain.review.entity;

import com.mopl.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reviews", indexes = {
        @Index(name = "idx_review_content_created", columnList = "content_id, created_at DESC")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "texts", nullable = false, length = 255)
    private String text;

    @Column(nullable = false)
    private double rating;

    public Review(Long userId, Long contentId, String text, double rating) {
        this.userId = userId;
        this.contentId = contentId;
        this.text = text;
        this.rating = rating;
    }

    public void update(String text, Double rating) {
        if (text != null && !text.isBlank()) {
            this.text = text.trim();
        }
        if (rating != null) {
            this.rating = rating;
        }
    }

    public boolean isAuthor(Long userId) {
        return userId != null && userId.equals(this.userId);
    }
}