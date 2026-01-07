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
import java.util.Objects;

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
                List.of()   // 콘텐츠 기능 미완성: 빈 리스트
        );
    }

    // 플레이리스트 단건 조회
    @Transactional(readOnly = true)
    public PlaylistDto find(Long requesterId, Long playlistId) {
        validateAuthenticated(requesterId);

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new MoplException(ErrorCode.NOT_FOUND));

        PlaylistDto.Owner owner = loadOwner(playlist.getUserId());
        boolean subscribedByMe = isSubscribedByMe(requesterId, playlist);

        return new PlaylistDto(
                playlist.getId(),
                owner,
                playlist.getTitle(),
                playlist.getDescription(),
                playlist.getUpdatedAt(),
                playlist.getSubscriberCount(),
                subscribedByMe,
                List.of() // 콘텐츠 기능 미완성: 빈 리스트
        );
    }

    // 플레이리스트 수정
    @Transactional
    public PlaylistDto update(Long requesterId, Long playlistId, PlaylistUpdateRequest request) {
        validateAuthenticated(requesterId);

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new MoplException(ErrorCode.NOT_FOUND));

        validateOwner(requesterId, playlist);

        String newTitle = request.title() != null ? request.title() : playlist.getTitle();
        String newDescription = request.description() != null ? request.description() : playlist.getDescription();

        playlist.update(newTitle, newDescription);

        return find(requesterId, playlistId);
    }

    // 플레이리스트 삭제 (TODO)
    @Transactional
    public void delete(Long requesterId, Long playlistId) {
        throw new UnsupportedOperationException("TODO: implement in next commits (delete)");
    }

    // 플레이리스트 구독 (TODO)
    @Transactional
    public void subscribe(Long requesterId, Long playlistId) {
        throw new UnsupportedOperationException("TODO: implement in next commits (subscribe)");
    }

    // 플레이리스트 구독 취소 (TODO)
    @Transactional
    public void unsubscribe(Long requesterId, Long playlistId) {
        throw new UnsupportedOperationException("TODO: implement in next commits (unsubscribe)");
    }

    // 플레이리스트에 콘텐츠 추가 (TODO)
    @Transactional
    public void addContent(Long requesterId, Long playlistId, Long contentId) {
        throw new UnsupportedOperationException("TODO: implement in next commits (addContent)");
    }

    // 플레이리스트에서 콘텐츠 삭제 (TODO)
    @Transactional
    public void removeContent(Long requesterId, Long playlistId, Long contentId) {
        throw new UnsupportedOperationException("TODO: implement in next commits (removeContent)");
    }

    // 플레이리스트 목록 조회 (커서 페이지네이션) (TODO)
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

    // 인증 체크
    private void validateAuthenticated(Long requesterId) {
        if (requesterId == null) {
            throw new MoplException(ErrorCode.UNAUTHORIZED);
        }
    }

    // 소유자 체크
    private void validateOwner(Long requesterId, Playlist playlist) {
        if (!Objects.equals(playlist.getUserId(), requesterId)) {
            throw new MoplException(ErrorCode.FORBIDDEN);
        }
    }

    // owner 로드 (없으면 null 필드로)
    private PlaylistDto.Owner loadOwner(Long ownerId) {
        User owner = userRepository.findById(ownerId).orElse(null);
        if (owner == null) {
            return new PlaylistDto.Owner(ownerId, null, null);
        }
        return new PlaylistDto.Owner(owner.getId(), owner.getName(), owner.getProfileImageUrl());
    }

    // 구독 여부
    private boolean isSubscribedByMe(Long requesterId, Playlist playlist) {
        return Objects.equals(playlist.getUserId(), requesterId);
    }
}