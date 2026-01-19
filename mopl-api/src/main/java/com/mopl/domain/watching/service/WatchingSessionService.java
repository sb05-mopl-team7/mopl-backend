package com.mopl.domain.watching.service;

import com.mopl.domain.content.dto.ContentDto;
import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.user.dto.response.UserSummaryDto;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.domain.watching.dto.response.WatchingSessionContentListResponse;
import com.mopl.domain.watching.dto.response.WatchingSessionUserResponse;
import com.mopl.domain.watching.entity.WatchingSession;
import com.mopl.domain.watching.exception.WatchingErrorCode;
import com.mopl.domain.watching.exception.WatchingException;
import com.mopl.global.enums.SortDirection;
import com.mopl.global.redis.RedisManager;
import com.mopl.global.redis.RedisNameSpace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WatchingSessionService {

    private final RedisManager redisManager;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;

    // 특정 사용자의 시청 세션 단건 조회
    public WatchingSessionUserResponse getWatchingSession(Long watcherId) {
        if (watcherId == null || watcherId < 0) {
            throw new WatchingException(WatchingErrorCode.INVALID_WATCHING_REQUEST);
        }

        if (!userRepository.existsById(watcherId)) {
            throw new WatchingException(WatchingErrorCode.USER_NOT_FOUND);
        }

        return redisManager.findHashByKey(RedisNameSpace.USER_WATCHING, String.valueOf(watcherId), WatchingSession.class)
                .map(session -> {
                    User user = userRepository.findById(watcherId)
                            .orElseThrow(() -> new WatchingException(WatchingErrorCode.USER_NOT_FOUND));
                    Content content = contentRepository.findById(session.getContentId())
                            .orElseThrow(() -> new WatchingException(WatchingErrorCode.CONTENT_NOT_FOUND));

                    return convertToResponse(session, user, content);
                })
                .orElse(null);
    }

    // 특정 콘텐츠의 시청 세션 목록 조회 (커서 페이지네이션)
    public WatchingSessionContentListResponse getWatchingSessionsByContent(
            Long contentId, String watcherNameLike, String cursor,
            Long idAfter, Integer limit, String sortBy, SortDirection sortDirection
    ) {
        // 1. 유효성 검사 - 커서 파싱 및 리밋 검증
        if (cursor != null && !cursor.isBlank()) {
            try {
                LocalDateTime.parse(cursor);
            } catch (DateTimeParseException e) {
                throw new WatchingException(WatchingErrorCode.INVALID_CURSOR);
            }
        }

        if (limit == null || limit <= 0 || limit > 100) {
            throw new WatchingException(WatchingErrorCode.INVALID_PAGINATION_LIMIT);
        }

        // 2. 콘텐츠 존재 확인
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new WatchingException(WatchingErrorCode.CONTENT_NOT_FOUND));

        // 3. Redis 데이터 조회
        Set<Long> watcherIds = redisManager.getSetMembers(RedisNameSpace.CONTENT_WATCHERS, String.valueOf(contentId), Long.class);
        if (watcherIds.isEmpty()) {
            return createEmptyResponse(sortBy, sortDirection);
        }

        List<WatchingSession> allSessions = watcherIds.stream()
                .map(id -> redisManager.findHashByKey(RedisNameSpace.USER_WATCHING, String.valueOf(id), WatchingSession.class))
                .flatMap(Optional::stream)
                .toList();

        // 4. RDB 벌크 조회를 통해 데이터 매핑
        List<Long> currentWatcherIds = allSessions.stream().map(WatchingSession::getId).toList();
        Map<Long, User> userMap = userRepository.findAllById(currentWatcherIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 5. 정렬 및 필터링
        List<WatchingSessionUserResponse> allResponses = allSessions.stream()
                .map(session -> {
                    User user = userMap.get(session.getId());
                    if (user == null) return null;
                    return convertToResponse(session, user, content);
                })
                .filter(Objects::nonNull)
                .filter(res -> watcherNameLike == null || res.watcher().name().contains(watcherNameLike))
                .sorted((o1, o2) -> {
                    int compare = sortDirection == SortDirection.ASCENDING
                            ? o1.createdAt().compareTo(o2.createdAt())
                            : o2.createdAt().compareTo(o1.createdAt());
                    return (compare != 0) ? compare : Long.valueOf(o1.id()).compareTo(Long.valueOf(o2.id()));
                })
                .toList();

        // 6. 페이지네이션 슬라이싱
        int startIndex = 0;
        if (cursor != null && idAfter != null) {
            startIndex = findStartIndex(allResponses, LocalDateTime.parse(cursor), idAfter, sortDirection);
        }

        int totalSize = allResponses.size();
        int endIndex = Math.min(startIndex + limit, totalSize);
        List<WatchingSessionUserResponse> pagedData = allResponses.subList(startIndex, endIndex);

        boolean hasNext = endIndex < totalSize;
        String nextCursor = (hasNext && !pagedData.isEmpty()) ? pagedData.get(pagedData.size() - 1).createdAt().toString() : null;
        String nextIdAfter = (hasNext && !pagedData.isEmpty()) ? pagedData.get(pagedData.size() - 1).id() : null;

        return WatchingSessionContentListResponse.builder()
                .data(pagedData).nextCursor(nextCursor).nextIdAfter(nextIdAfter).hasNext(hasNext)
                .totalCount(totalSize).sortBy(sortBy).sortDirection(sortDirection).build();
    }

    private WatchingSessionUserResponse convertToResponse(WatchingSession session, User user, Content content) {
        UserSummaryDto watcherDto = new UserSummaryDto(user.getId(), user.getName(), user.getProfileImageUrl());
        List<String> tags = content.getContentTags().stream().map(ct -> ct.getTag().getTag()).toList();
        ContentDto contentDto = new ContentDto(String.valueOf(content.getId()), content.getContentType(), content.getTitle(),
                content.getDescription(), content.getThumbnailUrl(), tags, content.getAverageRating(), content.getReviewCount(), 0);
        return WatchingSessionUserResponse.of(session, watcherDto, contentDto);
    }

    private int findStartIndex(List<WatchingSessionUserResponse> list, LocalDateTime cursorTime, Long idAfter, SortDirection direction) {
        for (int i = 0; i < list.size(); i++) {
            WatchingSessionUserResponse item = list.get(i);
            boolean isAfter = (direction == SortDirection.ASCENDING)
                    ? item.createdAt().isAfter(cursorTime) || (item.createdAt().isEqual(cursorTime) && Long.parseLong(item.id()) > idAfter)
                    : item.createdAt().isBefore(cursorTime) || (item.createdAt().isEqual(cursorTime) && Long.parseLong(item.id()) < idAfter);
            if (isAfter) return i;
        }
        return list.size();
    }

    private WatchingSessionContentListResponse createEmptyResponse(String sortBy, SortDirection direction) {
        return WatchingSessionContentListResponse.builder()
                .data(Collections.emptyList()).hasNext(false).totalCount(0).sortBy(sortBy).sortDirection(direction).build();
    }
}