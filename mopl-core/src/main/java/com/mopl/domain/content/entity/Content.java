package com.mopl.domain.content.entity;

import com.mopl.domain.content.enums.ContentType;
import com.mopl.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Content extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ContentType contentType;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String thumbnailUrl;

    private double averageRating;

    private int reviewCount;

    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContentTag> contentTags = new ArrayList<>();

    public Content(ContentType contentType, String title, String description, String thumbnailUrl) {
        this.contentType = contentType;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.averageRating = 0.0;
        this.reviewCount = 0;
    }

    public void addTag(Tag tag) {
        boolean isDuplicate = this.contentTags.stream()
                .anyMatch(ct -> ct.getTag().getId() != null &&
                        ct.getTag().getId().equals(tag.getId()));

        if (!isDuplicate) {
            ContentTag contentTag = new ContentTag(this, tag);
            this.contentTags.add(contentTag);
        }
    }

    public void update(String title, String description, String thumbnailUrl) {
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
    }

    public void updateReview(int reviewCount, double averageRating){
        this.reviewCount = reviewCount;
        this.averageRating = averageRating;
    }
}
