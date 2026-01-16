package com.mopl.domain.playlist.support;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.playlist.dto.response.PlaylistContentDto;
import com.mopl.domain.playlist.dto.response.PlaylistDto;
import com.mopl.domain.playlist.entity.Playlist;
import com.mopl.domain.playlist.entity.PlaylistContent;
import com.mopl.domain.playlist.entity.PlaylistSubscribe;
import com.mopl.domain.playlist.repository.PlaylistContentRepository;
import com.mopl.domain.playlist.repository.PlaylistSubscribeRepository;
import com.mopl.domain.user.dto.response.UserSummaryDto;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.s3.S3Manager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
public class PlaylistDtoAssembler {

    private static final int LIST_CONTENT_PREVIEW_LIMIT = 3;

    private final UserRepository userRepository;
    private final PlaylistSubscribeRepository playlistSubscribeRepository;
    private final PlaylistContentRepository playlistContentRepository;
    private final ContentRepository contentRepository;
    private final S3Manager s3Manager;

    //단건 조회: 콘텐츠 전체 내려줌
    public PlaylistDto toDto(Long requesterId, Playlist playlist) {
        UserSummaryDto owner = loadOwner(playlist.getUserId());
        boolean subscribedByMe = isSubscribedByMe(requesterId, playlist);
        List<PlaylistContentDto> contents = loadContentsByPlaylistId(playlist.getId()); // 전체

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

    //목록 조회: playlist당 콘텐츠 미리보기 3개만 내려줌(설명(description) 제외)
    public List<PlaylistDto> toDtoList(Long requesterId, List<Playlist> playlists) {
        if (playlists == null || playlists.isEmpty()) return List.of();

        List<Long> playlistIds = playlists.stream().map(Playlist::getId).toList();

        Set<Long> ownerIds = new HashSet<>();
        for (Playlist p : playlists) ownerIds.add(p.getUserId());

        Map<Long, UserSummaryDto> ownerMap = loadOwnerMap(ownerIds);
        Set<Long> subscribedPlaylistIds = loadSubscribedPlaylistIds(requesterId, playlistIds);

        // 목록에서는 preview만: playlist당 최대 3개 + description 제외
        Map<Long, List<PlaylistContentDto>> contentsMap =
                loadContentPreviewsByPlaylistIds(playlistIds, LIST_CONTENT_PREVIEW_LIMIT);

        List<PlaylistDto> result = new ArrayList<>();
        for (Playlist p : playlists) {
            UserSummaryDto owner = ownerMap.getOrDefault(
                    p.getUserId(),
                    new UserSummaryDto(p.getUserId(), null, null)
            );

            boolean subscribedByMe = requesterId != null
                    && (Objects.equals(p.getUserId(), requesterId) || subscribedPlaylistIds.contains(p.getId()));

            List<PlaylistContentDto> contents = contentsMap.getOrDefault(p.getId(), List.of());

            result.add(new PlaylistDto(
                    p.getId(),
                    owner,
                    p.getTitle(),
                    p.getDescription(),
                    p.getUpdatedAt(),
                    p.getSubscriberCount(),
                    subscribedByMe,
                    contents
            ));
        }
        return result;
    }

    private UserSummaryDto loadOwner(Long ownerId) {
        User owner = userRepository.findById(ownerId).orElse(null);
        if (owner == null) return new UserSummaryDto(ownerId, null, null);

        String profileUrl = presignIfS3(owner.getProfileImageUrl());
        return new UserSummaryDto(owner.getId(), owner.getName(), profileUrl);
    }

    private Map<Long, UserSummaryDto> loadOwnerMap(Set<Long> ownerIds) {
        if (ownerIds == null || ownerIds.isEmpty()) return Map.of();

        List<User> owners = userRepository.findAllById(ownerIds);

        Map<Long, UserSummaryDto> map = new HashMap<>();
        for (User u : owners) {
            String profileUrl = presignIfS3(u.getProfileImageUrl());
            map.put(u.getId(), new UserSummaryDto(u.getId(), u.getName(), profileUrl));
        }
        return map;
    }

    private boolean isSubscribedByMe(Long requesterId, Playlist playlist) {
        if (requesterId == null) return false;
        if (Objects.equals(playlist.getUserId(), requesterId)) return true;
        return playlistSubscribeRepository.existsByUserIdAndPlaylistId(requesterId, playlist.getId());
    }

    private Set<Long> loadSubscribedPlaylistIds(Long requesterId, List<Long> playlistIds) {
        if (requesterId == null || playlistIds == null || playlistIds.isEmpty()) return Set.of();

        List<PlaylistSubscribe> subs =
                playlistSubscribeRepository.findAllByUserIdAndPlaylistIdIn(requesterId, playlistIds);

        Set<Long> set = new HashSet<>();
        for (PlaylistSubscribe s : subs) set.add(s.getPlaylistId());
        return set;
    }

    //단건 조회용(전체)
    private List<PlaylistContentDto> loadContentsByPlaylistId(Long playlistId) {
        List<PlaylistContent> pcs = playlistContentRepository.findAllByPlaylistId(playlistId);
        if (pcs.isEmpty()) return List.of();

        pcs.sort(Comparator.comparing(PlaylistContent::getId));

        List<Long> contentIds = pcs.stream().map(PlaylistContent::getContentId).toList();
        Map<Long, Content> contentMap = loadContentMap(contentIds);

        List<PlaylistContentDto> result = new ArrayList<>();
        for (Long contentId : contentIds) {
            Content content = contentMap.get(contentId);
            if (content == null) continue;
            result.add(toContentDto(content, true)); // description 포함
        }
        return result;
    }

    //목록 조회용(playlist당 N개 preview)
    private Map<Long, List<PlaylistContentDto>> loadContentPreviewsByPlaylistIds(List<Long> playlistIds, int limitPerPlaylist) {
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

        // playlist당 limitPerPlaylist까지만 contentId 수집(그 이상은 무시)
        for (PlaylistContent pc : pcs) {
            List<Long> ids = playlistToContentIds.computeIfAbsent(pc.getPlaylistId(), k -> new ArrayList<>());
            if (ids.size() >= limitPerPlaylist) continue;

            ids.add(pc.getContentId());
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
                dtos.add(toContentDto(content, false));
            }
            result.put(playlistId, dtos);
        }
        return result;
    }

    private Map<Long, Content> loadContentMap(Collection<Long> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) return Map.of();

        List<Content> fetched = contentRepository.findAllByIdInWithTags(contentIds);

        Map<Long, Content> map = new LinkedHashMap<>();
        for (Content c : fetched) map.putIfAbsent(c.getId(), c);
        return map;
    }

    private PlaylistContentDto toContentDto(Content content, boolean includeDescription) {
        List<String> tags = content.getContentTags().stream()
                .map(ct -> ct.getTag().getTag())
                .distinct()
                .toList();

        String thumbnailUrl = presignIfS3(content.getThumbnailUrl());

        return new PlaylistContentDto(
                content.getId(),
                content.getContentType() == null ? null : content.getContentType().name(),
                content.getTitle(),
                includeDescription ? content.getDescription() : null,
                thumbnailUrl,
                tags,
                content.getAverageRating(),
                content.getReviewCount()
        );
    }

    private String presignIfS3(String value) {
        if (value == null || value.isBlank()) return null;

        if (value.contains("X-Amz-Signature=")) return value;

        if (value.startsWith("http")) {
            if (isS3Url(value)) {
                String key = extractS3Key(value);
                return s3Manager.generatePresignedUrl(key);
            }
            return value;
        }

        return s3Manager.generatePresignedUrl(value);
    }

    private boolean isS3Url(String url) {
        try {
            URI uri = URI.create(url);
            String host = (uri.getHost() == null) ? "" : uri.getHost().toLowerCase();
            return host.contains("amazonaws.com") && (host.contains(".s3.") || host.startsWith("s3."));
        } catch (Exception e) {
            // 파싱 실패 시 보수적으로 문자열 기준 체크
            String lower = url.toLowerCase();
            return lower.contains("amazonaws.com") && (lower.contains(".s3.") || lower.contains("s3."));
        }
    }

    private String extractS3Key(String s3Url) {
        try {
            URI uri = URI.create(s3Url);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            String path = uri.getPath();

            if (path == null || path.isBlank()) return s3Url;

            String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8);
            String key = decoded.startsWith("/") ? decoded.substring(1) : decoded;

            if (host.startsWith("s3.") || host.startsWith("s3-") || host.equals("s3.amazonaws.com")) {
                int firstSlash = key.indexOf('/');
                if (firstSlash > 0 && firstSlash < key.length() - 1) {
                    key = key.substring(firstSlash + 1);
                }
            }

            return key.isBlank() ? s3Url : key;
        } catch (Exception e) {
            return s3Url;
        }
    }
}