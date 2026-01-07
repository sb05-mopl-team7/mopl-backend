package com.mopl.domain.playlist.service;

import com.mopl.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.domain.playlist.dto.response.PlaylistDto;
import com.mopl.domain.playlist.entity.Playlist;
import com.mopl.domain.playlist.repository.PlaylistRepository;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.dto.PageResponse;
import com.mopl.global.enums.SortDirection;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;

    // 플레이리스트 생성
    @Transactional
    public PlaylistDto create(Long requesterId, PlaylistCreateRequest request) {
        validateAuthenticated(requesterId);

        Playlist playlist = new Playlist(requesterId, request.title(), request.description());
        Playlist saved = playlistRepository.save(playlist);

        PlaylistDto.Owner owner = loadOwner(saved.getUserId());

        return new PlaylistDto(
                saved.getId(),
                owner,
                saved.getTitle(),
                saved.getDescription(),
                saved.getUpdatedAt(),
                saved.getSubscriberCount(),
                true,
                List.of()      // 콘텐츠 기능 미완성: 빈 리스트
        );
    }

    // ===== 아래부터는 기능별로 구현 예정 컴파일용 스텁 =====

    @Transactional(readOnly = true)
    public PlaylistDto find(Long requesterId, Long playlistId) {
        throw new UnsupportedOperationException("TODO: implement in next commits (find)");
    }

    @Transactional
    public PlaylistDto update(Long requesterId, Long playlistId, PlaylistUpdateRequest request) {
        throw new UnsupportedOperationException("TODO: implement in next commits (update)");
    }

    @Transactional
    public void delete(Long requesterId, Long playlistId) {
        throw new UnsupportedOperationException("TODO: implement in next commits (delete)");
    }

    @Transactional
    public void subscribe(Long requesterId, Long playlistId) {
        throw new UnsupportedOperationException("TODO: implement in next commits (subscribe)");
    }

    @Transactional
    public void unsubscribe(Long requesterId, Long playlistId) {
        throw new UnsupportedOperationException("TODO: implement in next commits (unsubscribe)");
    }

    @Transactional
    public void addContent(Long requesterId, Long playlistId, Long contentId) {
        throw new UnsupportedOperationException("TODO: implement in next commits (addContent)");
    }

    @Transactional
    public void removeContent(Long requesterId, Long playlistId, Long contentId) {
        throw new UnsupportedOperationException("TODO: implement in next commits (removeContent)");
    }

    @Transactional(readOnly = true)
    public PageResponse<PlaylistDto> findAll(
            Long requesterId,
            String keywordLike,
            Long ownerIdEqual,
            Long subscriberIdEqual,
            String cursor,
            String idAfter,
            Integer limit,
            String sortBy,
            SortDirection sortDirection
    ) {
        throw new UnsupportedOperationException("TODO: implement in next commits (findAll)");
    }

    // ===== 공통 헬퍼 =====

    private void validateAuthenticated(Long requesterId) {
        if (requesterId == null) {
            throw new MoplException(ErrorCode.UNAUTHORIZED);
        }
    }

    private PlaylistDto.Owner loadOwner(Long ownerId) {
        User owner = userRepository.findById(ownerId).orElse(null);
        if (owner == null) {
            return new PlaylistDto.Owner(ownerId, null, null);
        }
        return new PlaylistDto.Owner(owner.getId(), owner.getName(), owner.getProfileImageUrl());
    }
}
