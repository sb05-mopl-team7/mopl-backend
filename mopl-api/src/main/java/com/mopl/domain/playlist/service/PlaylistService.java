package com.mopl.domain.playlist.service;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.domain.playlist.dto.response.PlaylistContentDto;
import com.mopl.domain.playlist.dto.response.PlaylistDto;
import com.mopl.domain.playlist.dto.response.PlaylistOwnerDto;
import com.mopl.domain.playlist.entity.Playlist;
import com.mopl.domain.playlist.entity.PlaylistContent;
import com.mopl.domain.playlist.entity.PlaylistSubscribe;
import com.mopl.domain.playlist.repository.PlaylistContentRepository;
import com.mopl.domain.playlist.repository.PlaylistRepository;
import com.mopl.domain.playlist.repository.PlaylistSubscribeRepository;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.dto.PageResponse;
import com.mopl.global.enums.SortDirection;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.s3.S3Manager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PlaylistService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final PlaylistRepository playlistRepository;
    private final PlaylistSubscribeRepository playlistSubscribeRepository;
    private final UserRepository userRepository;
    private final PlaylistContentRepository playlistContentRepository;
    private final ContentRepository contentRepository;

    // S3 presigned 변환용 (응답에서만 변환)
    private final S3Manager s3Manager;

    // 플레이리스트 생성
    @Transactional
    public PlaylistDto create(Long requesterId, PlaylistCreateRequest request) {
        validateAuthenticated(requesterId);

        Playlist playlist = new Playlist(requesterId, request.title(), request.description());
        Playlist saved = playlistRepository.save(playlist);

        PlaylistOwnerDto owner = loadOwner(saved.getUserId());

        return new PlaylistDto(
                saved.getId(),
                owner,
                saved.getTitle(),
                saved.getDescription(),
                saved.getUpdatedAt(),
                saved.getSubscriberCount(),
                true,
                List.of()
        );
    }

    // 플레이리스트 단건 조회
    @Transactional(readOnly = true)
    public PlaylistDto find(Long requesterId, Long playlistId) {
        validateAuthenticated(requesterId);

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new MoplException(ErrorCode.NOT_FOUND));

        PlaylistOwnerDto owner = loadOwner(playlist.getUserId());
        boolean subscribedByMe = isSubscribedByMe(requesterId, playlist);
        List<PlaylistContentDto> contents = loadContentsByPlaylistId(playlist.getId());

        return new PlaylistDto(
                playlist.getId(),
                owner,
                playlist.getTitle(),
                playlist.getDescription(),
                playlist.getUpdatedAt(),
                playlist.getSubscriberCount(),
                subscribedByMe,
                contents
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
        if (Objects.equals(playlist.getUserId(), requesterId)) {
            return;
        }
        if (playlistSubscribeRepository.existsByUserIdAndPlaylistId(requesterId, playlistId)) {
            return;
        }
        playlistSubscribeRepository.save(new PlaylistSubscribe(requesterId, playlistId));
        // Race Condition 방지: 엔티티 더티체킹 대신 원자 UPDATE 사용
        playlistRepository.increaseSubscriberCount(playlistId);
    }

    // 플레이리스트 구독 취소
    @Transactional
    public void unsubscribe(Long requesterId, Long playlistId) {
        validateAuthenticated(requesterId);

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new MoplException(ErrorCode.NOT_FOUND));
        if (Objects.equals(playlist.getUserId(), requesterId)) {
            return;
        }

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
        int size = normalizeLimit(limit);

        String normalizedSortBy = normalizeSortBy(sortBy);
        SortDirection normalizedDirection = (sortDirection == null) ? SortDirection.DESCENDING : sortDirection;

        CursorKey key = parseCursorKey(cursor, idAfter, normalizedSortBy);

        // limit+1 조회로 hasNext 판별
        List<Playlist> fetched = playlistRepository.cursorFindAll(
                keywordLike,
                ownerIdEqual,
                subscriberIdEqual,
                key.cursorUpdatedAt,
                key.cursorSubscriberCount,
                key.idAfter,
                size + 1,
                normalizedSortBy,
                normalizedDirection
        );

        boolean hasNext = fetched.size() > size;
        List<Playlist> page = hasNext ? fetched.subList(0, size) : fetched;

        List<Long> playlistIds = page.stream().map(Playlist::getId).toList();

        Map<Long, PlaylistOwnerDto> ownerMap = loadOwnerMap(
                page.stream().map(Playlist::getUserId).collect(java.util.stream.Collectors.toSet())
        );

        Set<Long> subscribedPlaylistIds = loadSubscribedPlaylistIds(requesterId, playlistIds);

        Map<Long, List<PlaylistContentDto>> contentsMap = loadContentsByPlaylistIds(playlistIds);

        List<PlaylistDto> data = page.stream().map(p -> {
            PlaylistOwnerDto owner = ownerMap.getOrDefault(
                    p.getUserId(),
                    new PlaylistOwnerDto(p.getUserId(), null, null)
            );

            boolean subscribedByMe = requesterId != null
                    && (Objects.equals(p.getUserId(), requesterId) || subscribedPlaylistIds.contains(p.getId()));

            List<PlaylistContentDto> contents = contentsMap.getOrDefault(p.getId(), List.of());

            return new PlaylistDto(
                    p.getId(),
                    owner,
                    p.getTitle(),
                    p.getDescription(),
                    p.getUpdatedAt(),
                    p.getSubscriberCount(),
                    subscribedByMe,
                    contents
            );
        }).toList();

        String nextCursor = null;
        Long nextIdAfter = null;

        if (hasNext && !page.isEmpty()) {
            Playlist last = page.get(page.size() - 1);
            nextIdAfter = last.getId();

            if ("updatedAt".equalsIgnoreCase(normalizedSortBy)) {
                nextCursor = formatDateTimeCursor(last.getUpdatedAt());
            } else {
                nextCursor = String.valueOf(last.getSubscriberCount());
            }
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

    // contents 로딩/매핑
    private List<PlaylistContentDto> loadContentsByPlaylistId(Long playlistId) {
        List<PlaylistContent> pcs = playlistContentRepository.findAllByPlaylistId(playlistId);
        if (pcs.isEmpty()) return List.of();

        pcs.sort(Comparator.comparing(PlaylistContent::getId));

        List<Long> contentIds = pcs.stream()
                .map(PlaylistContent::getContentId)
                .toList();

        Map<Long, Content> contentMap = loadContentMap(contentIds);

        List<PlaylistContentDto> result = new ArrayList<>();
        for (Long contentId : contentIds) {
            Content content = contentMap.get(contentId);
            if (content == null) continue;
            result.add(toPlaylistContentDto(content));
        }
        return result;
    }

    // 목록 조회용
    private Map<Long, List<PlaylistContentDto>> loadContentsByPlaylistIds(List<Long> playlistIds) {
        if (playlistIds == null || playlistIds.isEmpty()) return Map.of();

        List<PlaylistContent> pcs = playlistContentRepository.findAllByPlaylistIdIn(playlistIds);
        if (pcs.isEmpty()) {
            Map<Long, List<PlaylistContentDto>> empty = new HashMap<>();
            for (Long pid : playlistIds) empty.put(pid, List.of());
            return empty;
        }

        pcs.sort(Comparator.comparing(PlaylistContent::getId));

        Map<Long, List<Long>> playlistToContentIds = new HashMap<>();
        Set<Long> allContentIds = new LinkedHashSet<>();

        for (PlaylistContent pc : pcs) {
            playlistToContentIds.computeIfAbsent(pc.getPlaylistId(), k -> new ArrayList<>())
                    .add(pc.getContentId());
            allContentIds.add(pc.getContentId());
        }

        Map<Long, Content> contentMap = loadContentMap(allContentIds);

        Map<Long, List<PlaylistContentDto>> result = new HashMap<>();
        for (Long playlistId : playlistIds) {
            List<Long> contentIds = playlistToContentIds.getOrDefault(playlistId, List.of());
            if (contentIds.isEmpty()) {
                result.put(playlistId, List.of());
                continue;
            }

            List<PlaylistContentDto> dtos = new ArrayList<>();
            for (Long contentId : contentIds) {
                Content content = contentMap.get(contentId);
                if (content == null) continue;
                dtos.add(toPlaylistContentDto(content));
            }
            result.put(playlistId, dtos);
        }
        return result;
    }

    private Map<Long, Content> loadContentMap(Collection<Long> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) return Map.of();

        List<Content> fetched = contentRepository.findAllByIdInWithTags(contentIds);

        Map<Long, Content> map = new LinkedHashMap<>();
        for (Content c : fetched) {
            map.putIfAbsent(c.getId(), c);
        }
        return map;
    }

    private PlaylistContentDto toPlaylistContentDto(Content content) {
        List<String> tags = content.getContentTags().stream()
                .map(ct -> ct.getTag().getTag())
                .distinct()
                .toList();

        // 썸네일도 응답에서 presigned로 변환
        String thumbnailUrl = presignIfS3(content.getThumbnailUrl());

        return new PlaylistContentDto(
                content.getId(),
                content.getContentType() == null ? null : content.getContentType().name(),
                content.getTitle(),
                content.getDescription(),
                thumbnailUrl,
                tags,
                content.getAverageRating(),
                content.getReviewCount()
        );
    }

    // owner
    private Map<Long, PlaylistOwnerDto> loadOwnerMap(Set<Long> ownerIds) {
        if (ownerIds == null || ownerIds.isEmpty()) return Map.of();

        List<User> owners = userRepository.findAllById(ownerIds);

        Map<Long, PlaylistOwnerDto> map = new HashMap<>();
        for (User u : owners) {
            String profileUrl = presignIfS3(u.getProfileImageUrl());
            map.put(u.getId(), new PlaylistOwnerDto(u.getId(), u.getName(), profileUrl));
        }
        return map;
    }

    private Set<Long> loadSubscribedPlaylistIds(Long requesterId, List<Long> playlistIds) {
        if (requesterId == null || playlistIds == null || playlistIds.isEmpty()) return Set.of();

        List<PlaylistSubscribe> subs =
                playlistSubscribeRepository.findAllByUserIdAndPlaylistIdIn(requesterId, playlistIds);

        Set<Long> set = new HashSet<>();
        for (PlaylistSubscribe s : subs) {
            set.add(s.getPlaylistId());
        }
        return set;
    }

    // cursor
    private int normalizeLimit(Integer limit) {
        if (limit == null) return DEFAULT_LIMIT;
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }
        return limit;
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return "updatedAt";

        String v = sortBy.trim();
        if ("updatedAt".equalsIgnoreCase(v)) return "updatedAt";
        if ("subscribeCount".equalsIgnoreCase(v) || "subscriberCount".equalsIgnoreCase(v)) {
            return "subscriberCount";
        }

        throw new MoplException(ErrorCode.INVALID_REQUEST);
    }

    private CursorKey parseCursorKey(String cursorRaw, String idAfterRaw, String normalizedSortBy) {
        boolean hasCursor = cursorRaw != null && !cursorRaw.isBlank();
        boolean hasIdAfter = idAfterRaw != null && !idAfterRaw.isBlank();

        if (hasCursor != hasIdAfter) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }

        if (!hasCursor) {
            return new CursorKey(null, null, null);
        }

        Long parsedIdAfter = parseLong(idAfterRaw);

        if ("updatedAt".equalsIgnoreCase(normalizedSortBy)) {
            LocalDateTime updatedAt = parseDateTimeCursor(cursorRaw);
            return new CursorKey(updatedAt, null, parsedIdAfter);
        } else {
            Long subscriberCount = parseLong(cursorRaw);
            return new CursorKey(null, subscriberCount, parsedIdAfter);
        }
    }

    private Long parseLong(String raw) {
        try {
            return Long.parseLong(raw.trim());
        } catch (Exception e) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }
    }

    private LocalDateTime parseDateTimeCursor(String raw) {
        String normalized = raw.trim().replace(" ", "T");
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }
    }

    private String formatDateTimeCursor(LocalDateTime value) {
        return value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private record CursorKey(
            LocalDateTime cursorUpdatedAt,
            Long cursorSubscriberCount,
            Long idAfter
    ) {}

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

    private PlaylistOwnerDto loadOwner(Long ownerId) {
        User owner = userRepository.findById(ownerId).orElse(null);
        if (owner == null) {
            return new PlaylistOwnerDto(ownerId, null, null);
        }
        String profileUrl = presignIfS3(owner.getProfileImageUrl());
        return new PlaylistOwnerDto(owner.getId(), owner.getName(), profileUrl);
    }

    private boolean isSubscribedByMe(Long requesterId, Playlist playlist) {
        if (Objects.equals(playlist.getUserId(), requesterId)) {
            return true;
        }
        return playlistSubscribeRepository.existsByUserIdAndPlaylistId(requesterId, playlist.getId());
    }

    /**
     * [P3] 프로필/썸네일 공통 presigned 처리: S3 리소스면 presigned URL로 변환한다.
     */
    private String presignIfS3(String keyOrUrl) {
        if (keyOrUrl == null || keyOrUrl.isBlank()) return null;

        // 이미 presigned면 그대로 반환
        if (keyOrUrl.contains("X-Amz-Signature=")) {
            return keyOrUrl;
        }

        // http인데 S3 도메인이 아니면(외부 URL) 그대로 반환
        if (keyOrUrl.startsWith("http")
                && !(keyOrUrl.contains("amazonaws.com")
                || keyOrUrl.contains(".s3.")
                || keyOrUrl.contains("s3.ap-"))) {
            return keyOrUrl;
        }

        return s3Manager.generatePresignedUrl(keyOrUrl);
    }
}
