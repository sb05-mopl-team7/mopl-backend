package com.mopl.domain.playlist.service;

import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.domain.playlist.dto.response.PlaylistDto;
import com.mopl.domain.playlist.entity.Playlist;
import com.mopl.domain.playlist.entity.PlaylistContent;
import com.mopl.domain.playlist.entity.PlaylistSubscribe;
import com.mopl.domain.playlist.repository.PlaylistContentRepository;
import com.mopl.domain.playlist.repository.PlaylistRepository;
import com.mopl.domain.playlist.repository.PlaylistSubscribeRepository;
import com.mopl.domain.playlist.support.PlaylistCursorSupport;
import com.mopl.domain.playlist.support.PlaylistDtoAssembler;
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
    private final PlaylistSubscribeRepository playlistSubscribeRepository;
    private final PlaylistContentRepository playlistContentRepository;
    private final ContentRepository contentRepository;

    private final PlaylistDtoAssembler playlistDtoAssembler;

    // 플레이리스트 생성
    @Transactional
    public PlaylistDto create(Long requesterId, PlaylistCreateRequest request) {
        validateAuthenticated(requesterId);

        Playlist playlist = new Playlist(requesterId, request.title(), request.description());
        Playlist saved = playlistRepository.save(playlist);

        return playlistDtoAssembler.toDto(requesterId, saved);
    }

    // 플레이리스트 단건 조회
    @Transactional(readOnly = true)
    public PlaylistDto find(Long requesterId, Long playlistId) {
        validateAuthenticated(requesterId);

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new MoplException(ErrorCode.NOT_FOUND));

        return playlistDtoAssembler.toDto(requesterId, playlist);
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

        return playlistDtoAssembler.toDto(requesterId, playlist);
    }

    // 플레이리스트 삭제
    @Transactional
    public void delete(Long requesterId, Long playlistId) {
        validateAuthenticated(requesterId);

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new MoplException(ErrorCode.NOT_FOUND));

        validateOwner(requesterId, playlist);
        playlistRepository.delete(playlist);
    }

    // 플레이리스트 구독
    @Transactional
    public void subscribe(Long requesterId, Long playlistId) {
        validateAuthenticated(requesterId);

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new MoplException(ErrorCode.NOT_FOUND));

        if (Objects.equals(playlist.getUserId(), requesterId)) return;
        if (playlistSubscribeRepository.existsByUserIdAndPlaylistId(requesterId, playlistId)) return;

        playlistSubscribeRepository.save(new PlaylistSubscribe(requesterId, playlistId));
        playlistRepository.increaseSubscriberCount(playlistId);
    }

    // 플레이리스트 구독 취소
    @Transactional
    public void unsubscribe(Long requesterId, Long playlistId) {
        validateAuthenticated(requesterId);

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new MoplException(ErrorCode.NOT_FOUND));

        if (Objects.equals(playlist.getUserId(), requesterId)) return;

        long deleted = playlistSubscribeRepository.deleteByUserIdAndPlaylistId(requesterId, playlistId);
        if (deleted > 0) {
            playlistRepository.decreaseSubscriberCount(playlistId);
        }
    }

    // 플레이리스트 콘텐츠 추가
    @Transactional
    public void addContent(Long requesterId, Long playlistId, Long contentId) {
        validateAuthenticated(requesterId);

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new MoplException(ErrorCode.NOT_FOUND));

        validateOwner(requesterId, playlist);

        if (!contentRepository.existsById(contentId)) {
            throw new MoplException(ErrorCode.NOT_FOUND);
        }
        if (playlistContentRepository.existsByPlaylistIdAndContentId(playlistId, contentId)) {
            return;
        }
        playlistContentRepository.save(new PlaylistContent(playlistId, contentId));
    }

    // 플레이리스트에서 콘텐츠 삭제
    @Transactional
    public void removeContent(Long requesterId, Long playlistId, Long contentId) {
        validateAuthenticated(requesterId);

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new MoplException(ErrorCode.NOT_FOUND));

        validateOwner(requesterId, playlist);

        if (!playlistContentRepository.existsByPlaylistIdAndContentId(playlistId, contentId)) {
            return;
        }
        playlistContentRepository.deleteByPlaylistIdAndContentId(playlistId, contentId);
    }

    // 플레이리스트 목록 조회 (커서 페이지네이션)
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
        int size = PlaylistCursorSupport.normalizeLimit(limit);

        String normalizedSortBy = PlaylistCursorSupport.normalizeSortBy(sortBy);
        SortDirection normalizedDirection = PlaylistCursorSupport.normalizeDirection(sortDirection);

        PlaylistCursorSupport.CursorKey key =
                PlaylistCursorSupport.parseCursorKey(cursor, idAfter, normalizedSortBy);

        List<Playlist> fetched = playlistRepository.cursorFindAll(
                keywordLike,
                ownerIdEqual,
                subscriberIdEqual,
                key.cursorUpdatedAt(),
                key.cursorSubscriberCount(),
                key.idAfter(),
                size + 1,
                normalizedSortBy,
                normalizedDirection
        );

        boolean hasNext = fetched.size() > size;
        List<Playlist> page = hasNext ? fetched.subList(0, size) : fetched;

        List<PlaylistDto> data = playlistDtoAssembler.toDtoList(requesterId, page);

        String nextCursor = null;
        Long nextIdAfter = null;

        if (hasNext && !page.isEmpty()) {
            Playlist last = page.get(page.size() - 1);
            nextIdAfter = last.getId();
            nextCursor = PlaylistCursorSupport.nextCursor(normalizedSortBy, last);
        }

        return PageResponse.<PlaylistDto>builder()
                .data(data)
                .nextCursor(nextCursor)
                .nextIdAfter(nextIdAfter)
                .hasNext(hasNext)
                .totalCount(0L)
                .sortBy(normalizedSortBy)
                .sortDirection(normalizedDirection)
                .build();
    }

    // 인증/인가
    private void validateAuthenticated(Long requesterId) {
        if (requesterId == null) {
            throw new MoplException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void validateOwner(Long requesterId, Playlist playlist) {
        if (!Objects.equals(playlist.getUserId(), requesterId)) {
            throw new MoplException(ErrorCode.FORBIDDEN);
        }
    }
}