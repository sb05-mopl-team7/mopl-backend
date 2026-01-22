package com.mopl.domain.notification.controller;

import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.conversation.dto.response.DirectMessageDto;
import com.mopl.domain.conversation.service.DirectMessageService;
import com.mopl.domain.notification.dto.NotificationDto;
import com.mopl.domain.notification.service.NotificationService;
import com.mopl.global.sse.SseManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseManager sseManager;
    private final DirectMessageService directMessageService;
    private final NotificationService notificationService;

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                @RequestParam(value = "LastEventId", required = false) String lastEventId) {

        SseEmitter emitter = sseManager.subscribe(userPrincipal.getUserId());

        // 재연결 요청시 유실 데이터 전송
        if (lastEventId != null && !lastEventId.isBlank()) {
            resendMissedEvents(userPrincipal.getUserId(), lastEventId);
        }

        return emitter;
    }

    private void resendMissedEvents(Long userId, String lastEventId) {
        try {
            // ID 형식: {timestamp}_TYPE_{dataId}
            String[] parts = lastEventId.split("_");

            if (parts.length < 3) {
                log.warn("잘못된 형식의 LastEventId: {}", lastEventId);
                return;
            }

            long lastTimestamp = Long.parseLong(parts[0]);
            String type = parts[1];
            long lastId = Long.parseLong(parts[2]);

            // 유실된 DM 조회 및 재전송
            if ("DM".equals(type)) {
                // 마지막이 DM이었음 -> DM은 ID까지 정밀 조회
                List<DirectMessageDto> missedDms = directMessageService.findMissedMessages(userId, lastTimestamp, lastId);
                sendDms(userId, missedDms);

                // 알림은 시간으로만 조회 (ID 비교 불가하므로)
                List<NotificationDto> missedNotis = notificationService.findMissedNotifications(userId, lastTimestamp, 0L);
                sendNotifications(userId, missedNotis);
            } else if ("NOTI".equals(type)) {
                // 마지막이 알림이었음 -> 알림은 ID까지 정밀 조회
                List<NotificationDto> missedNotis = notificationService.findMissedNotifications(userId, lastTimestamp, lastId);
                sendNotifications(userId, missedNotis);

                // DM은 시간으로만 조회
                List<DirectMessageDto> missedDms = directMessageService.findMissedMessages(userId, lastTimestamp, 0L);
                sendDms(userId, missedDms);
            }
        } catch (Exception e) {
            log.error("유실 데이터 전송 중 오류: {}", e.getMessage());
        }
    }

    private void sendDms(Long userId, List<DirectMessageDto> dms) {
        for (DirectMessageDto dm : dms) {
            sseManager.sendToUser(userId, "direct-messages", dm);
        }
    }

    private void sendNotifications(Long userId, List<NotificationDto> notis) {
        for (NotificationDto noti : notis) {
            sseManager.sendToUser(userId, "notifications", noti);
        }
    }
}
