package com.mopl.domain.contents.entity;

import com.mopl.domain.contents.enums.ContentType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Contents {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Enumerated(EnumType.STRING)
    private ContentType contentType;

    private String title;

    private String description;

    private String thumbnailUrl;

    private double averageRating;

    private int reviewCount;

    public Contents(ContentType contentType, String title, String description, String thumbnailUrl) {
        this.contentType = contentType;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.averageRating = 0;
        this.reviewCount = 0;
    }
}
