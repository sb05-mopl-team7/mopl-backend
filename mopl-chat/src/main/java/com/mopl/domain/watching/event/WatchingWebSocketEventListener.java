package com.mopl.domain.watching.event;

import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.watching.service.WatchingPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingWebSocketEventListener {

    private static final String WATCHING_PREFIX = "/contents/";
    private static final String KEY_CURRENT_CONTENT_ID = "current_content_id";

    private final WatchingPresenceService watchingPresenceService;

    // 사용자가 콘텐츠에 입장할 때
    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination(); // SUBSCRIBE /sub/contents/{contentId}/watch

        if (destination == null || !destination.contains(WATCHING_PREFIX)) {
            return;
        }

        String sessionId = headerAccessor.getSessionId();
        Long userId = extractUserId(headerAccessor);
        Long contentId = extractContentId(destination);

        // 메모리에 세션 매핑 저장 - 콘텐츠 나갈 때 제거하기 위해
        if (sessionId != null && contentId != null && headerAccessor.getSessionAttributes() != null) {
            headerAccessor.getSessionAttributes().put(KEY_CURRENT_CONTENT_ID, contentId);

            watchingPresenceService.joinContent(userId, contentId);
        }
    }

    // 사용자가 콘텐츠를 나감
    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
        handleLeave(event.getMessage());
    }

    // 사용자가 앱을 나감
    @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event) {
        handleLeave(event.getMessage());
    }

    private void handleLeave(Message<byte[]> message) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(message);

        Long contentId = null;
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null) {
            contentId = (Long) sessionAttributes.get(KEY_CURRENT_CONTENT_ID);
        }

        Long userId = extractUserId(headerAccessor);

        if (contentId != null && userId != null) {
            watchingPresenceService.leaveContent(userId, contentId);

            sessionAttributes.remove(KEY_CURRENT_CONTENT_ID);
        }
    }

    private Long extractUserId(StompHeaderAccessor headerAccessor) {
        try {
            if (headerAccessor.getUser() instanceof Authentication authentication) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof UserPrincipal userPrincipal) {
                    return userPrincipal.userId();
                }
            }
        } catch (Exception e) {
            log.error("WebSocket userId 추출 실패", e);
        }
        return null;
    }

    private Long extractContentId(String destination) {
        try {
            String[] parts = destination.split("/");

            for (int i = 0; i < parts.length; i++) {
                if ("contents".equals(parts[i]) && i + 1 < parts.length) {
                    return Long.parseLong(parts[i + 1]);
                }
            }
        } catch (Exception e) {
            log.error("WebSocket contentId 추출 실패: {}", destination);
        }
        return null;
    }
}
