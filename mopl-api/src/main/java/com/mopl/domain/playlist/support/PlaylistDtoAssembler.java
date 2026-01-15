package com.mopl.domain.playlist.support;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.playlist.dto.response.PlaylistContentDto;
import com.mopl.domain.playlist.dto.response.PlaylistDto;
import com.mopl.domain.playlist.dto.response.PlaylistOwnerDto;
import com.mopl.domain.playlist.entity.Playlist;
import com.mopl.domain.playlist.entity.PlaylistContent;
import com.mopl.domain.playlist.entity.PlaylistSubscribe;
import com.mopl.domain.playlist.repository.PlaylistContentRepository;
import com.mopl.domain.playlist.repository.PlaylistSubscribeRepository;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.s3.S3Manager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class PlaylistDtoAssembler {

    private final UserRepository userRepository;
    private final PlaylistSubscribeRepository playlistSubscribeRepository;
    private final PlaylistContentRepository playlistContentRepository;
    private final ContentRepository contentRepository;
    private final S3Manager s3Manager;

    public PlaylistDto toDto(Long requesterId, Playlist playlist) {
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

    public List<PlaylistDto> toDtoList(Long requesterId, List<Playlist> playlists) {
        if (playlists == null || playlists.isEmpty()) return List.of();

        List<Long> playlistIds = playlists.stream().map(Playlist::getId).toList();

        Set<Long> ownerIds = new HashSet<>();
        for (Playlist p : playlists) ownerIds.add(p.getUserId());

        Map<Long, PlaylistOwnerDto> ownerMap = loadOwnerMap(ownerIds);
        Set<Long> subscribedPlaylistIds = loadSubscribedPlaylistIds(requesterId, playlistIds);
        Map<Long, List<PlaylistContentDto>> contentsMap = loadContentsByPlaylistIds(playlistIds);

        List<PlaylistDto> result = new ArrayList<>();
        for (Playlist p : playlists) {
            PlaylistOwnerDto owner = ownerMap.getOrDefault(
                    p.getUserId(),
                    new PlaylistOwnerDto(p.getUserId(), null, null)
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

    private PlaylistOwnerDto loadOwner(Long ownerId) {
        User owner = userRepository.findById(ownerId).orElse(null);
        if (owner == null) return new PlaylistOwnerDto(ownerId, null, null);

        String profileUrl = presignIfS3(owner.getProfileImageUrl());
        return new PlaylistOwnerDto(owner.getId(), owner.getName(), profileUrl);
    }

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
            result.add(toContentDto(content));
        }
        return result;
    }

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
                dtos.add(toContentDto(content));
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

    private PlaylistContentDto toContentDto(Content content) {
        List<String> tags = content.getContentTags().stream()
                .map(ct -> ct.getTag().getTag())
                .distinct()
                .toList();

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

    private String presignIfS3(String keyOrUrl) {
        if (keyOrUrl == null || keyOrUrl.isBlank()) return null;

        if (keyOrUrl.contains("X-Amz-Signature=")) {
            return keyOrUrl;
        }

        if (keyOrUrl.startsWith("http")
                && !(keyOrUrl.contains("amazonaws.com")
                || keyOrUrl.contains(".s3.")
                || keyOrUrl.contains("s3.ap-"))) {
            return keyOrUrl;
        }

        return s3Manager.generatePresignedUrl(keyOrUrl);
    }
}