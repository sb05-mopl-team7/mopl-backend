package com.mopl.domain.playlist.repository;

import com.mopl.domain.playlist.entity.PlaylistSubscribe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 특정 플레이리스트를 구독한 유저들의 id 조회
    @Query("""
        SELECT ps.userId FROM PlaylistSubscribe ps
        WHERE ps.playlistId = :playlistId
    """)
    List<Long> findUserIdsByPlaylistId(@Param("playlistId") Long playlistId);
}