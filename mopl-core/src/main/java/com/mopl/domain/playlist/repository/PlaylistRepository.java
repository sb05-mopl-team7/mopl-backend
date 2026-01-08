package com.mopl.domain.playlist.repository;

import com.mopl.domain.playlist.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaylistRepository extends JpaRepository<Playlist, Long>, PlaylistRepositoryCustom {

    boolean existsByIdAndUserId(Long id, Long userId);

    Optional<Playlist> findByIdAndUserId(Long id, Long userId);
}
