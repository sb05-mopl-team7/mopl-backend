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
        // 1. 요청 파라미터 유효성 검증
        if (watcherId == null || watcherId < 0) {
            throw new WatchingException(WatchingErrorCode.INVALID_WATCHING_REQUEST);
        }

        // 2. DB에 해당 사용자가 존재하는지 확인
        if (!userRepository.existsById(watcherId)) {
            throw new WatchingException(WatchingErrorCode.USER_NOT_FOUND);
        }

        // 3. Redis Hash(USER_WATCHING)에서 세션 데이터(contentId, createdAt 등)를 O(1)로 직접 조회
        return redisManager.findHashByKey(RedisNameSpace.USER_WATCHING, String.valueOf(watcherId), WatchingSession.class)
                .map(session -> {
                    // 1-4. Redis 데이터가 존재할 경우, RDB에서 유저 및 콘텐츠 상세 정보 조회
                    User user = userRepository.findById(watcherId)
                            .orElseThrow(() -> new WatchingException(WatchingErrorCode.USER_NOT_FOUND));
                    Content content = contentRepository.findById(session.getContentId())
                            .orElseThrow(() -> new WatchingException(WatchingErrorCode.CONTENT_NOT_FOUND));

                    // 1-5. 조회된 데이터를 결합하여 최종 응답 DTO로 변환
                    return convertToResponse(session, user, content);
                })
                .orElse(null);
    }

    // 특정 콘텐츠의 시청 세션 목록 조회 (커서 페이지네이션)
    public WatchingSessionContentListResponse getWatchingSessionsByContent(
            Long contentId, String watcherNameLike, String cursor,
            Long idAfter, Integer limit, String sortBy, SortDirection sortDirection
    ) {
        // 1. 페이지네이션 Limit 유효성 검사
        if (limit == null || limit <= 0) {
            throw new WatchingException(WatchingErrorCode.INVALID_PAGINATION_LIMIT);
        }

        // 2. Redis Set(CONTENT_WATCHERS)에서 해당 콘텐츠를 시청 중인 모든 유저 ID 목록 조회
        Set<Long> watcherIds = redisManager.getSetMembers(RedisNameSpace.CONTENT_WATCHERS, String.valueOf(contentId), Long.class);
        if (watcherIds.isEmpty()) {
            return createEmptyResponse(sortBy, sortDirection);
        }

        // 3. 조회된 유저 ID들을 기반으로 Redis Hash에서 각각의 상세 세션 정보(createdAt 등) 조회
        List<WatchingSession> allSessions = watcherIds.stream()
                .map(id -> redisManager.findHashByKey(RedisNameSpace.USER_WATCHING, String.valueOf(id), WatchingSession.class))
                .flatMap(Optional::stream)
                .toList();

        // 4. RDB 데이터 벌크 조회를 통해 엔티티 매핑 (N+1 문제 방지)
        List<Long> currentWatcherIds = allSessions.stream().map(WatchingSession::getId).toList();
        Map<Long, User> userMap = userRepository.findAllById(currentWatcherIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new WatchingException(WatchingErrorCode.CONTENT_NOT_FOUND));

        // 5. 데이터 결합, 이름 필터링 및 API 명세에 따른 정렬(createdAt 기준) 수행
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
                    return (compare != 0) ? compare : o1.id().compareTo(o2.id());
                })
                .toList();

        // 6. 커서(createdAt, ID) 기반으로 페이징 시작 위치 탐색
        int startIndex = 0;
        if (cursor != null && idAfter != null) {
            try {
                LocalDateTime cursorTime = LocalDateTime.parse(cursor);
                startIndex = findStartIndex(allResponses, cursorTime, idAfter, sortDirection);
            } catch (Exception e) {
                throw new WatchingException(WatchingErrorCode.INVALID_CURSOR);
            }
        }

        // 7. 전체 리스트에서 해당 페이지의 데이터만큼 슬라이싱
        int totalSize = allResponses.size();
        int endIndex = Math.min(startIndex + limit, totalSize);
        List<WatchingSessionUserResponse> pagedData = allResponses.subList(startIndex, endIndex);

        // 8. 다음 페이지 존재 여부 및 커서 정보 생성
        boolean hasNext = endIndex < totalSize;
        String nextCursor = null;
        String nextIdAfter = null;

        if (hasNext && !pagedData.isEmpty()) {
            WatchingSessionUserResponse lastItem = pagedData.get(pagedData.size() - 1);
            nextCursor = lastItem.createdAt().toString();
            nextIdAfter = lastItem.id();
        }

        return WatchingSessionContentListResponse.builder()
                .data(pagedData)
                .nextCursor(nextCursor)
                .nextIdAfter(nextIdAfter)
                .hasNext(hasNext)
                .totalCount(totalSize)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
    }

    //응답 DTO 변환
    private WatchingSessionUserResponse convertToResponse(WatchingSession session, User user, Content content) {
        // 1. 유저 정보 요약 DTO 생성
        UserSummaryDto watcherDto = new UserSummaryDto(
                user.getId(),
                user.getName(),
                user.getProfileImageUrl()
        );

        // 2. 콘텐츠 태그 리스트 변환
        List<String> tags = content.getContentTags().stream()
                .map(ct -> ct.getTag().getTag())
                .toList();

        // 3. 콘텐츠 상세 정보 DTO 생성
        ContentDto contentDto = new ContentDto(
                String.valueOf(content.getId()),
                content.getContentType(),
                content.getTitle(),
                content.getDescription(),
                content.getThumbnailUrl(),
                tags,
                content.getAverageRating(),
                content.getReviewCount(),
                0
        );

        return WatchingSessionUserResponse.of(session, watcherDto, contentDto);
    }

    // 페이징 시작 인덱스 탐색
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

    // 빈 응답 객체 생성
    private WatchingSessionContentListResponse createEmptyResponse(String sortBy, SortDirection direction) {
        return WatchingSessionContentListResponse.builder()
                .data(Collections.emptyList())
                .hasNext(false)
                .totalCount(0)
                .sortBy(sortBy)
                .sortDirection(direction)
                .build();
    }
}