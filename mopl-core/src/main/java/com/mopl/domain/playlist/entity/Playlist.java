package com.mopl.domain.playlist.entity;

import com.mopl.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "playlists", indexes = {
        @Index(name = "idx_playlist_user_created", columnList = "user_id, created_at DESC")
})
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

    @Column(name = "subscriber_count", nullable = false)
    private long subscriberCount;

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
        this.subscriberCount++;
    }

    public void decreaseSubscriberCount() {
        if (this.subscriberCount > 0L) {
            this.subscriberCount--;
        }
    }
}