package com.mopl.domain.watching.service;

import com.mopl.domain.content.dto.ContentDto;
import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.user.dto.response.UserSummaryDto;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.domain.watching.dto.response.WatchingSessionResponse;
import com.mopl.domain.watching.entity.WatchingSession;
import com.mopl.domain.watching.exception.WatchingErrorCode;
import com.mopl.domain.watching.exception.WatchingException;
import com.mopl.domain.watching.repository.WatchingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WatchingSessionService {

    private final WatchingSessionRepository watchingSessionRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;

    public WatchingSessionResponse getWatchingSession(Long watcherId) {
        // 1. 비즈니스 유효성 검증 (순서가 중요: DB 조회보다 먼저 수행)
        if (watcherId == null || watcherId < 0) {
            throw new WatchingException(WatchingErrorCode.INVALID_WATCHING_REQUEST);
        }

        // 2. 유저 존재 확인
        if (!userRepository.existsById(watcherId)) {
            throw new WatchingException(WatchingErrorCode.USER_NOT_FOUND);
        }

        // 3. Redis에서 세션 조회 (없으면 null 반환)
        return watchingSessionRepository.findById(watcherId)
                .map(this::convertToResponse)
                .orElse(null);
    }

    private WatchingSessionResponse convertToResponse(WatchingSession session) {
        // 엔티티 조회
        User user = userRepository.findById(session.getId())
                .orElseThrow(() -> new WatchingException(WatchingErrorCode.USER_NOT_FOUND));

        Content content = contentRepository.findById(session.getContentId())
                .orElseThrow(() -> new WatchingException(WatchingErrorCode.CONTENT_NOT_FOUND));

        // 1. UserSummaryDto 매핑
        UserSummaryDto watcherDto = new UserSummaryDto(
                user.getId(),
                user.getName(),
                user.getProfileImageUrl()
        );

        // 2. ContentDto 매핑
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

        return WatchingSessionResponse.of(session, watcherDto, contentDto);
    }
}