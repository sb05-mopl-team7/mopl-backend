package com.mopl.domain.directmessage.event;

import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.global.redis.RedisManager;
import com.mopl.global.redis.RedisNameSpace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventLister {

    private final String CONVERSATION_PREFIX = "/conversations/";
    private final String CURRENT_ROOM_ID = "currentRoomId";

    private final RedisManager redisManager;

    // 사용자가 채팅방에 들어올 때
    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String destination = headerAccessor.getDestination();
        String userId = extractUserId(headerAccessor);

        // destination = /sub/conversations/{conversationId}/direct-messages
        if (userId != null && destination != null && destination.contains(CONVERSATION_PREFIX)) {
            String conversationId = extractConversationId(destination);
            if (conversationId == null) {
                return;
            }

            redisManager.addSetElement(RedisNameSpace.DM_VIEWERS, conversationId, userId);

            // Disconnect 시 사용하기 위해 세션에 방 id 저장
            if (headerAccessor.getSessionAttributes() != null) {
                headerAccessor.getSessionAttributes().put(CURRENT_ROOM_ID, conversationId);
            }

            log.info("사용자 {}가 대화방 {}에 입장", userId, conversationId);
        }
    }

    // 사용자가 채팅방을 나감
    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        removeUserFromRoom(headerAccessor);
    }

    // 사용자가 앱을 나감
    @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        removeUserFromRoom(headerAccessor);
    }

    private void removeUserFromRoom(StompHeaderAccessor headerAccessor) {
        String userId = extractUserId(headerAccessor);
        String conversationId = headerAccessor.getSessionAttributes() != null ?
                (String) headerAccessor.getSessionAttributes().get(CURRENT_ROOM_ID)
                : null;

        if (userId != null && conversationId != null) {
            redisManager.removeSetElement(RedisNameSpace.DM_VIEWERS, conversationId, userId);
            log.info("사용자 {}가 대화방 {}에서 퇴장.", userId, conversationId);
        }
    }

    private String extractUserId(StompHeaderAccessor headerAccessor) {
        try {
            if (headerAccessor.getUser() instanceof Authentication authentication) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof UserPrincipal userPrincipal) {
                    return String.valueOf(userPrincipal.userId());
                }
            }
        } catch (Exception e) {
            log.error("WebSocket userId 추출 실패", e);
        }
        return null;
    }

    private String extractConversationId(String destination) {
        // "/sub/conversations/10/direct-messages" -> "10"
        try {
            int start = destination.indexOf(CONVERSATION_PREFIX);
            if (start == -1) {
                return null;
            }

            start += CONVERSATION_PREFIX.length();
            int end = destination.indexOf("/direct-messages", start);
            if (end == -1) {
                return null;
            }

            return destination.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }
}
