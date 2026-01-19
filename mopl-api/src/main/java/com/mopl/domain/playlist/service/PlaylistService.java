package com.mopl.domain.playlist.service;

import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.follow.repository.FollowRepository;
import com.mopl.domain.notification.enums.NotificationType;
import com.mopl.domain.notification.producer.NotificationEventProducer;
import com.mopl.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.domain.playlist.dto.request.PlaylistSearchCondition;
import com.mopl.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.domain.playlist.dto.response.PlaylistDto;
import com.mopl.domain.playlist.entity.Playlist;
import com.mopl.domain.playlist.entity.PlaylistContent;
import com.mopl.domain.playlist.entity.PlaylistSubscribe;
import com.mopl.domain.playlist.exception.PlaylistErrorCode;
import com.mopl.domain.playlist.exception.PlaylistException;
import com.mopl.domain.playlist.repository.PlaylistContentRepository;
import com.mopl.domain.playlist.repository.PlaylistRepository;
import com.mopl.domain.playlist.repository.PlaylistSubscribeRepository;
import com.mopl.domain.playlist.support.PlaylistDtoAssembler;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.exception.UserErrorCode;
import com.mopl.domain.user.exception.UserException;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.dto.PageResponse;
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
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final NotificationEventProducer notificationEventProducer;

    private final PlaylistDtoAssembler playlistDtoAssembler;

    // 플레이리스트 생성
    @Transactional
    public PlaylistDto create(Long requesterId, PlaylistCreateRequest request) {
        Playlist playlist = new Playlist(requesterId, request.title(), request.description());
        Playlist saved = playlistRepository.save(playlist);

        // 나를 팔로우하는 사람들에게 알림 발행
        User user = userRepository.findById(requesterId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_EXIST));

        List<Long> followerIds = followRepository.findFollowsByFolloweeId(requesterId);
        for (Long followerId : followerIds) {
            notificationEventProducer.send(
                    followerId,
                    NotificationType.FOLLOWING_ACTIVITY_PLAYLIST,
                    user.getName(),
                    playlist.getTitle(),
                    playlist.getDescription()
            );
        }
        return playlistDtoAssembler.toDto(requesterId, saved);
    }

    // 플레이리스트 단건 조회
    @Transactional(readOnly = true)
    public PlaylistDto find(Long requesterId, Long playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new PlaylistException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        return playlistDtoAssembler.toDto(requesterId, playlist);
    }

    // 플레이리스트 수정
    @Transactional
    public PlaylistDto update(Long requesterId, Long playlistId, PlaylistUpdateRequest request) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new PlaylistException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        validateOwner(requesterId, playlist);

        String newTitle = request.title() != null ? request.title() : playlist.getTitle();
        String newDescription = request.description() != null ? request.description() : playlist.getDescription();
        playlist.update(newTitle, newDescription);

        return playlistDtoAssembler.toDto(requesterId, playlist);
    }

    // 플레이리스트 삭제 (연관 데이터 정리)
    @Transactional
    public void delete(Long requesterId, Long playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new PlaylistException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        validateOwner(requesterId, playlist);

        playlistSubscribeRepository.deleteAllByPlaylistId(playlistId);
        playlistContentRepository.deleteAllByPlaylistId(playlistId);
        playlistRepository.delete(playlist);
    }

    // 플레이리스트 구독
    @Transactional
    public void subscribe(Long requesterId, Long playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new PlaylistException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        if (Objects.equals(playlist.getUserId(), requesterId)) return;
        if (playlistSubscribeRepository.existsByUserIdAndPlaylistId(requesterId, playlistId)) return;

        playlistSubscribeRepository.save(new PlaylistSubscribe(requesterId, playlistId));
        playlistRepository.increaseSubscriberCount(playlistId);

        // 다른 사용자가 내 플레이리스트 구독시 알림 발행
        Long ownerId = playlist.getUserId();
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_EXIST));

        notificationEventProducer.send(
                ownerId,
                NotificationType.PLAYLIST_SUBSCRIBED,
                requester.getName(),
                playlist.getTitle());
    }

    // 플레이리스트 구독 취소
    @Transactional
    public void unsubscribe(Long requesterId, Long playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new PlaylistException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

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
                .orElseThrow(() -> new PlaylistException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        validateOwner(requesterId, playlist);

        if (!contentRepository.existsById(contentId)) {
            throw new PlaylistException(PlaylistErrorCode.CONTENT_NOT_FOUND);
        }
        if (playlistContentRepository.existsByPlaylistIdAndContentId(playlistId, contentId)) return;

        playlistContentRepository.save(new PlaylistContent(playlistId, contentId));

        // 구독중인 플레이리스트에 콘텐츠 추가되는 경우 알림 발행
        Long ownerId = playlist.getUserId();
        List<Long> subscriberIds = playlistSubscribeRepository.findUserIdsByPlaylistId(playlistId);
        for (Long subscriberId : subscriberIds) {
            if (subscriberId.equals(ownerId)) {
                continue;
            }

            notificationEventProducer.send(subscriberId, NotificationType.PLAYLIST_CONTENT_ADDED, playlist.getTitle());
        }
    }

    // 플레이리스트에서 콘텐츠 삭제
    @Transactional
    public void removeContent(Long requesterId, Long playlistId, Long contentId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new PlaylistException(PlaylistErrorCode.PLAYLIST_NOT_FOUND));

        validateOwner(requesterId, playlist);

        if (!playlistContentRepository.existsByPlaylistIdAndContentId(playlistId, contentId)) return;
        playlistContentRepository.deleteByPlaylistIdAndContentId(playlistId, contentId);
    }

    // 플레이리스트 목록 조회 (커서 페이지네이션)
    @Transactional(readOnly = true)
    public PageResponse<PlaylistDto> findAll(Long requesterId, PlaylistSearchCondition condition) {
        int size = condition.limit();
        String sortBy = condition.sortBy();
        var direction = condition.sortDirection();

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

        long totalCount = (long) page.size();

        String nextCursor = null;
        Long nextIdAfter = null;

        if (hasNext && !page.isEmpty()) {
            Playlist last = page.get(page.size() - 1);
            nextIdAfter = last.getId();
            nextCursor = condition.nextCursorOf(last);
        }

        return PageResponse.<PlaylistDto>builder()
                .data(data)
                .nextCursor(nextCursor)
                .nextIdAfter(nextIdAfter)
                .hasNext(hasNext)
                .totalCount(totalCount)
                .sortBy(sortBy)
                .sortDirection(direction)
                .build();
    }

    // 인가(Owner 체크)
    private void validateOwner(Long requesterId, Playlist playlist) {
        if (!Objects.equals(playlist.getUserId(), requesterId)) {
            throw new PlaylistException(PlaylistErrorCode.PLAYLIST_FORBIDDEN);
        }
    }
}