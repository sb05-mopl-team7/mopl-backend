package com.mopl.domain.playlist.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "playlist_subscribes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PlaylistSubscribe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "playlist_id", nullable = false)
    private Long playlistId;

    public PlaylistSubscribe(Long userId, Long playlistId) {
        this.userId = userId;
        this.playlistId = playlistId;
    }
}
