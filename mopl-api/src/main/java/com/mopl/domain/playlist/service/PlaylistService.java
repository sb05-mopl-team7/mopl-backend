package com.mopl.domain.playlist.service;

import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.domain.playlist.dto.request.PlaylistSearchCondition;
import com.mopl.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.domain.playlist.dto.response.PlaylistDto;
import com.mopl.domain.playlist.entity.Playlist;
import com.mopl.domain.playlist.entity.PlaylistContent;
import com.mopl.domain.playlist.entity.PlaylistSubscribe;
import com.mopl.domain.playlist.repository.PlaylistContentRepository;
import com.mopl.domain.playlist.repository.PlaylistRepository;
import com.mopl.domain.playlist.repository.PlaylistSubscribeRepository;
import com.mopl.domain.playlist.support.PlaylistDtoAssembler;
import com.mopl.global.dto.PageResponse;
import com.mopl.global.enums.SortDirection;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
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
        Playlist playlist = new Playlist(requesterId, request.title(), request.description());
        Playlist saved = playlistRepository.save(playlist);

        return playlistDtoAssembler.toDto(requesterId, saved);
    }

    // 플레이리스트 단건 조회
    @Transactional(readOnly = true)
    public PlaylistDto find(Long requesterId, Long playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new MoplException(ErrorCode.NOT_FOUND));

        return playlistDtoAssembler.toDto(requesterId, playlist);
    }

    // 플레이리스트 수정
    @Transactional
    public PlaylistDto update(Long requesterId, Long playlistId, PlaylistUpdateRequest request) {
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
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new MoplException(ErrorCode.NOT_FOUND));

        validateOwner(requesterId, playlist);

        // 잔여 데이터 정리 (FK 없거나 cascade 없을 때 안전)
        playlistSubscribeRepository.deleteAllByPlaylistId(playlistId);
        playlistContentRepository.deleteAllByPlaylistId(playlistId);

        playlistRepository.delete(playlist);
    }

    // 플레이리스트 구독
    @Transactional
    public void subscribe(Long requesterId, Long playlistId) {
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
    public PageResponse<PlaylistDto> findAll(Long requesterId, PlaylistSearchCondition condition) {
        int size = condition.normalizedLimit();
        String sortBy = condition.normalizedSortBy();
        SortDirection direction = condition.normalizedDirection();

        PlaylistSearchCondition.CursorKey key = condition.toCursorKey();

        List<Playlist> fetched = playlistRepository.cursorFindAll(
                condition.keywordLike(),
                condition.ownerIdEqual(),
                condition.subscriberIdEqual(),
                key.cursorUpdatedAt(),
                key.cursorSubscriberCount(),
                key.idAfter(),
                size + 1,
                sortBy,
                direction
        );

        boolean hasNext = fetched.size() > size;
        List<Playlist> page = hasNext ? fetched.subList(0, size) : fetched;

        List<PlaylistDto> data = playlistDtoAssembler.toDtoList(requesterId, page);

        String nextCursor = null;
        Long nextIdAfter = null;

        if (hasNext && !page.isEmpty()) {
            Playlist last = page.get(page.size() - 1);
            nextIdAfter = last.getId();

            // sortBy가 updatedAt이면 날짜 커서, 아니면 subscriberCount 커서
            nextCursor = "updatedAt".equalsIgnoreCase(sortBy)
                    ? last.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    : String.valueOf(last.getSubscriberCount());
        }

        return PageResponse.<PlaylistDto>builder()
                .data(data)
                .nextCursor(nextCursor)
                .nextIdAfter(nextIdAfter)
                .hasNext(hasNext)
                .totalCount(0L)
                .sortBy(sortBy)
                .sortDirection(direction)
                .build();
    }

    // 인가(Owner 체크)
    private void validateOwner(Long requesterId, Playlist playlist) {
        if (!Objects.equals(playlist.getUserId(), requesterId)) {
            throw new MoplException(ErrorCode.FORBIDDEN);
        }
    }
}
