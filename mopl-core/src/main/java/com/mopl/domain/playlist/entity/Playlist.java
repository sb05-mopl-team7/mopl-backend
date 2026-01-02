package com.mopl.domain.playlist.entity;

import com.mopl.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "playlists")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Playlist extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "subscriber_count")
    private Long subscriberCount;

    public Playlist(Long userId, String title, String description) {
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.subscriberCount = 0L;
    }

    public void update(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public void increaseSubscriberCount() {
        if (subscriberCount == null) subscriberCount = 0L;
        subscriberCount++;
    }

    public void decreaseSubscriberCount() {
        if (subscriberCount == null) subscriberCount = 0L;
        if (subscriberCount > 0) subscriberCount--;
    }
}
