package com.mopl.domain.playlist.repository;

import com.mopl.domain.playlist.entity.PlaylistContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlaylistContentRepository extends JpaRepository<PlaylistContent, Long> {

    List<PlaylistContent> findAllByPlaylistId(Long playlistId);

    List<PlaylistContent> findAllByPlaylistIdIn(List<Long> playlistIds);

    boolean existsByPlaylistIdAndContentId(Long playlistId, Long contentId);

    void deleteByPlaylistIdAndContentId(Long playlistId, Long contentId);

    void deleteAllByPlaylistId(Long playlistId);
}
