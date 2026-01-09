package com.mopl.domain.playlist.repository;

import com.mopl.domain.playlist.entity.PlaylistSubscribe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlaylistSubscribeRepository extends JpaRepository<PlaylistSubscribe, Long> {

    boolean existsByUserIdAndPlaylistId(Long userId, Long playlistId);

    Optional<PlaylistSubscribe> findByUserIdAndPlaylistId(Long userId, Long playlistId);

    long deleteByUserIdAndPlaylistId(Long userId, Long playlistId);

    List<PlaylistSubscribe> findAllByUserId(Long userId);

    List<PlaylistSubscribe> findAllByUserIdAndPlaylistIdIn(Long userId, List<Long> playlistIds);

    long deleteAllByPlaylistId(Long playlistId);

    long countByPlaylistId(Long playlistId);
}