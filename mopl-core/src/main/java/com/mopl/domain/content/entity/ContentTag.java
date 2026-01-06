package com.mopl.domain.content.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "content_tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id")
    private Content content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    private Tag tag;

    public ContentTag(Content content, Tag tag) {
        this.content = content;
        this.tag = tag;
    }
}