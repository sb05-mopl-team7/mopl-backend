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
import com.mopl.domain.watching.repository.WatchingSessionRepository;
import com.mopl.global.enums.SortDirection;
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

    private final WatchingSessionRepository watchingSessionRepository;
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

        return watchingSessionRepository.findById(watcherId)
                .map(this::convertToResponse)
                .orElse(null);
    }

    // 시청 세션 단건 변환 (개별 조회용)
    private WatchingSessionUserResponse convertToResponse(WatchingSession session) {
        User user = userRepository.findById(session.getId())
                .orElseThrow(() -> new WatchingException(WatchingErrorCode.USER_NOT_FOUND));

        Content content = contentRepository.findById(session.getContentId())
                .orElseThrow(() -> new WatchingException(WatchingErrorCode.CONTENT_NOT_FOUND));

        return convertToResponse(session, user, content);
    }

    // 시청 세션 엔티티와 RDB 데이터를 결합하여 응답 DTO로 변환
    private WatchingSessionUserResponse convertToResponse(WatchingSession session, User user, Content content) {
        UserSummaryDto watcherDto = new UserSummaryDto(
                user.getId(),
                user.getName(),
                user.getProfileImageUrl()
        );

        List<String> tags = content.getContentTags().stream()
                .map(contentTag -> contentTag.getTag().getTag())
                .toList();

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

    // 특정 콘텐츠의 시청 세션 목록 조회 (커서 페이지네이션 적용)
    public WatchingSessionContentListResponse getWatchingSessionsByContent(
            Long contentId, String watcherNameLike, String cursor,
            Long idAfter, Integer limit, String sortBy, SortDirection sortDirection
    ) {
        // 1. 페이지 크기 유효성 검사
        if (limit == null || limit <= 0) {
            throw new WatchingException(WatchingErrorCode.INVALID_PAGINATION_LIMIT);
        }

        // 2. Redis에서 해당 콘텐츠를 시청 중인 모든 세션 조회 (Secondary Index 활용)
        List<WatchingSession> allSessions = watchingSessionRepository.findAllByContentId(contentId);
        if (allSessions.isEmpty()) {
            return createEmptyResponse(sortBy, sortDirection);
        }

        // 3. 세션에 포함된 유저 및 콘텐츠 ID를 추출하여 DB 쿼리 최소화
        List<Long> watcherIds = allSessions.stream().map(WatchingSession::getId).toList();
        List<Long> contentIds = allSessions.stream().map(WatchingSession::getContentId).distinct().toList();

        // 4. 조회된 엔티티를 Map으로 변환
        Map<Long, User> userMap = userRepository.findAllById(watcherIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        Map<Long, Content> contentMap = contentRepository.findAllById(contentIds).stream()
                .collect(Collectors.toMap(Content::getId, content -> content));

        // 5. 엔티티 결합, 필터링 및 전체 정렬 수행
        List<WatchingSessionUserResponse> allResponses = allSessions.stream()
                .map(session -> {
                    User user = userMap.get(session.getId());
                    Content content = contentMap.get(session.getContentId());
                    if (user == null || content == null) return null; // 데이터 불일치 시 제외
                    return convertToResponse(session, user, content);
                })
                .filter(Objects::nonNull)
                // 이름 부분 일치 검색 필터링
                .filter(res -> watcherNameLike == null || res.watcher().name().contains(watcherNameLike))
                // 커서 기반 정렬 (createdAt 우선, 동일 시간일 경우 id 기준 정렬)
                .sorted((o1, o2) -> {
                    int compare = sortDirection == SortDirection.ASCENDING
                            ? o1.createdAt().compareTo(o2.createdAt())
                            : o2.createdAt().compareTo(o1.createdAt());
                    return (compare != 0) ? compare : o1.id().compareTo(o2.id());
                })
                .toList();

        // 6. 커서 위치 탐색 및 페이지 슬라이싱
        int startIndex = 0;
        if (cursor != null && idAfter != null) {
            try {
                LocalDateTime cursorTime = LocalDateTime.parse(cursor);
                startIndex = findStartIndex(allResponses, cursorTime, idAfter, sortDirection);
            } catch (Exception e) {
                throw new WatchingException(WatchingErrorCode.INVALID_CURSOR);
            }
        }

        int totalSize = allResponses.size();
        int endIndex = Math.min(startIndex + limit, totalSize);
        List<WatchingSessionUserResponse> pagedData = allResponses.subList(startIndex, endIndex);

        // 7. 다음 페이지 존재 여부 확인 및 다음 커서 정보 생성
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

    // 커서(시간, ID) 값을 기준으로 다음 페이지가 시작될 인덱스를 검색
    private int findStartIndex(List<WatchingSessionUserResponse> list, LocalDateTime cursorTime, Long idAfter, SortDirection direction) {
        for (int i = 0; i < list.size(); i++) {
            WatchingSessionUserResponse item = list.get(i);
            boolean isAfter;

            // 정렬 방향에 따라 커서보다 '뒤'에 있는 데이터인지 판단
            if (direction == SortDirection.ASCENDING) {
                isAfter = item.createdAt().isAfter(cursorTime) ||
                        (item.createdAt().isEqual(cursorTime) && Long.parseLong(item.id()) > idAfter);
            } else {
                isAfter = item.createdAt().isBefore(cursorTime) ||
                        (item.createdAt().isEqual(cursorTime) && Long.parseLong(item.id()) < idAfter);
            }

            if (isAfter) return i;
        }
        return list.size();
    }

    // 데이터가 없는 경우를 위한 빈 응답 객체 생성
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