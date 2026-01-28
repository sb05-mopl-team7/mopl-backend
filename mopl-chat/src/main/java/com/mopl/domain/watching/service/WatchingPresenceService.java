package com.mopl.domain.watching.service;

import com.mopl.domain.user.dto.response.UserSummaryDto;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.domain.watching.dto.WatchingSessionChange;
import com.mopl.domain.watching.dto.response.ContentPayload;
import com.mopl.domain.watching.dto.response.WatchingSessionEventPayload;
import com.mopl.domain.watching.entity.WatchingSession;
import com.mopl.domain.watching.enums.ChangeType;
import com.mopl.domain.watching.repository.WatchingSessionRepository;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.redis.RedisManager;
import com.mopl.global.redis.RedisNameSpace;
import com.mopl.global.s3.S3Manager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchingPresenceService {

    private final UserRepository userRepository;
    private final WatchingSessionRepository watchingSessionRepository;
    private final RedisManager redisManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final S3Manager s3Manager;

    public void joinContent(Long userId, Long contentId) {
        String userIdStr = String.valueOf(userId);
        String contentIdStr = String.valueOf(contentId);

        WatchingSession watchingSession = WatchingSession.builder()
                .id(userId)
                .contentId(contentId)
                .createdAt(LocalDateTime.now())
                .build();

        watchingSessionRepository.save(watchingSession);

        redisManager.addSetElement(RedisNameSpace.CONTENT_SESSIONS, contentIdStr, userIdStr);

        broadcast(userId, contentId, ChangeType.JOIN);
    }

    public void leaveContent(Long userId, Long contentId) {
        String userIdStr = String.valueOf(userId);
        String contentIdStr = String.valueOf(contentId);

        watchingSessionRepository.deleteById(userId);
        redisManager.removeSetElement(RedisNameSpace.CONTENT_SESSIONS, contentIdStr, userIdStr);

        broadcast(userId, contentId, ChangeType.LEAVE);
    }

    private void broadcast(Long userId, Long contentId, ChangeType changeType) {
        long currentCount = redisManager.getSetSize(RedisNameSpace.CONTENT_SESSIONS, String.valueOf(contentId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new MoplException(ErrorCode.NOT_FOUND));
        String profileUrl = s3Manager.generatePresignedUrl(user.getProfileImageUrl());

        WatchingSessionEventPayload payload = WatchingSessionEventPayload.builder()
                .id(userId)
                .createdAt(LocalDateTime.now())
                .watcher(new UserSummaryDto(
                        userId,
                        user.getName(),
                        profileUrl
                ))
                .content(ContentPayload.builder()
                        .id(String.valueOf(contentId))
                        .build())
                .build();

        WatchingSessionChange message = new WatchingSessionChange(changeType, payload, currentCount);

        // 해당 콘텐츠 접속 중인 유저들에게 전송
        String destination = "/sub/contents/" + contentId + "/watch";
        messagingTemplate.convertAndSend(destination, message);

        log.info("전송 {}: 유저 {}가 콘텐츠{}에 입장 (Total: {})", changeType, userId, contentId, currentCount);
    }
}
