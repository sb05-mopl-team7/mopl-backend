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
import com.mopl.global.s3.S3Manager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WatchingSessionService {

    private final WatchingSessionRepository watchingSessionRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final S3Manager s3Manager;

    /**
     * 특정 사용자의 시청 세션 단건 조회
     * @return 시청 중인 세션 정보 (없을 경우 null)
     */
    public WatchingSessionUserResponse getWatchingSession(Long watcherId) {
        if (watcherId == null || watcherId < 0) {
            throw new WatchingException(WatchingErrorCode.INVALID_WATCHING_REQUEST);
        }

        // 1. Redis에서 세션 존재 여부 확인
        return watchingSessionRepository.findById(watcherId)
                .map(session -> {
                    // 2. [최적화] 유저 정보 조회
                    User user = userRepository.findById(watcherId)
                            .orElseThrow(() -> new WatchingException(WatchingErrorCode.USER_NOT_FOUND));

                    // 3. [최적화] Fetch Join을 사용하여 태그 정보까지 한 번에 조회 (N+1 방지)
                    Content content = contentRepository.findByIdWithTags(session.getContentId())
                            .orElseThrow(() -> new WatchingException(WatchingErrorCode.CONTENT_NOT_FOUND));

                    return convertToResponse(session, user, content);
                })
                .orElse(null); // 시청 중이지 않으면 null 반환 (Controller에서 204 대응)
    }

    /**
     * 특정 콘텐츠의 시청 세션 목록 조회 (커서 페이지네이션 적용)
     */
    public WatchingSessionContentListResponse getWatchingSessionsByContent(
            Long contentId, String watcherNameLike, String cursor,
            Long idAfter, Integer limit, String sortBy, SortDirection sortDirection
    ) {
        // 1. Redis에서 해당 콘텐츠의 시청 세션 모두 조회
        List<WatchingSession> allSessions = watchingSessionRepository.findAllByContentId(contentId);
        if (allSessions.isEmpty()) {
            return createEmptyResponse(sortBy, sortDirection);
        }

        // 2. [최적화] 콘텐츠 정보는 단 1회만 조회 (Fetch Join 적용)
        Content content = contentRepository.findByIdWithTags(contentId)
                .orElseThrow(() -> new WatchingException(WatchingErrorCode.CONTENT_NOT_FOUND));

        // 3. [최적화] N+1 방지를 위해 시청자 정보를 In-clause로 일괄 조회
        List<Long> watcherIds = allSessions.stream().map(WatchingSession::getId).toList();
        Map<Long, User> userMap = userRepository.findAllById(watcherIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 4. 필터링 및 정렬 (Memory Level)
        List<WatchingSessionUserResponse> allResponses = allSessions.stream()
                .filter(session -> userMap.containsKey(session.getId()))
                .map(session -> convertToResponse(session, userMap.get(session.getId()), content))
                .filter(res -> watcherNameLike == null || res.watcher().name().contains(watcherNameLike))
                .sorted(getComparator(sortDirection))
                .toList();

        // 5. 커서 기반 슬라이싱 연산
        int startIndex = findStartIndex(allResponses, cursor, idAfter, sortDirection);
        int totalSize = allResponses.size();
        int endIndex = Math.min(startIndex + limit, totalSize);
        List<WatchingSessionUserResponse> pagedData = allResponses.subList(startIndex, endIndex);

        // 6. 다음 페이지 정보 생성
        boolean hasNext = endIndex < totalSize;
        String nextCursor = null;
        Long nextIdAfter = null;

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

    /** 커서(시간, ID) 값을 기준으로 슬라이싱 시작 인덱스 탐색 */
    private int findStartIndex(List<WatchingSessionUserResponse> list, String cursor, Long idAfter, SortDirection direction) {
        if (cursor == null || idAfter == null) return 0;

        try {
            LocalDateTime cursorTime = LocalDateTime.parse(cursor);
            for (int i = 0; i < list.size(); i++) {
                WatchingSessionUserResponse item = list.get(i);
                if (direction == SortDirection.ASCENDING) {
                    if (item.createdAt().isAfter(cursorTime) ||
                            (item.createdAt().isEqual(cursorTime) && item.id() > idAfter)) return i;
                } else {
                    if (item.createdAt().isBefore(cursorTime) ||
                            (item.createdAt().isEqual(cursorTime) && item.id() < idAfter)) return i;
                }
            }
        } catch (Exception e) {
            throw new WatchingException(WatchingErrorCode.INVALID_CURSOR);
        }
        return list.size();
    }

    /** 정렬 Comparator (createdAt -> id 순) */
    private Comparator<WatchingSessionUserResponse> getComparator(SortDirection direction) {
        return (o1, o2) -> {
            int compare = (direction == SortDirection.ASCENDING)
                    ? o1.createdAt().compareTo(o2.createdAt())
                    : o2.createdAt().compareTo(o1.createdAt());
            return (compare != 0) ? compare : o1.id().compareTo(o2.id());
        };
    }

    /** 엔티티 결합 및 DTO 변환 (패키지 구조 및 필드명 준수) */
    private WatchingSessionUserResponse convertToResponse(WatchingSession session, User user, Content content) {
        UserSummaryDto watcherDto = new UserSummaryDto(
                user.getId(),
                user.getName(),
                s3Manager.generatePresignedUrl(user.getProfileImageUrl())
        );

        // Fetch Join 덕분에 추가 쿼리 없이 태그 리스트 생성 가능
        List<String> tags = content.getContentTags().stream()
                .map(ct -> ct.getTag().getTag())
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

    private WatchingSessionContentListResponse createEmptyResponse(String sortBy, SortDirection direction) {
        return WatchingSessionContentListResponse.empty(sortBy, direction);
    }
}