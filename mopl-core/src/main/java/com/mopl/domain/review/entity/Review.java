package com.mopl.domain.review.entity;

import com.mopl.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reviews")
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
}
