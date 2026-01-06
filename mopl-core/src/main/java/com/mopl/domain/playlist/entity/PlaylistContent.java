package com.mopl.domain.playlist.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "playlist_contents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PlaylistContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "playlist_id", nullable = false)
    private Long playlistId;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    public PlaylistContent(Long playlistId, Long contentId) {
        this.playlistId = playlistId;
        this.contentId = contentId;
    }
}
