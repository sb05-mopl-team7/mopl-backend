package com.mopl.global.sse;

import com.mopl.domain.conversation.dto.response.DirectMessageDto;
import com.mopl.domain.notification.dto.NotificationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SseManager {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private static final Long DEFAULT_TIMEOUT = 45_000L; // 프론트의 재연결 주기 기준

    /**
     * 클라이언트와 서버 간의 SSE 파이프라인을 생성하고 관리합니다.
     * 생성된 연결은 실시간 채팅, 알림 서비스 등의 단방향 이벤트 전송에 활용됩니다.
     *
     * @param userId 연결을 식별할 사용자 ID
     * @return 실시간 통신을 위한 SseEmitter 객체
     */
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));

        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(userId);
        });

        emitter.onError((e) -> {
            emitter.complete();
            emitters.remove(userId);
        });

        // 더미 이벤트 전송 (503 에러 방지용)
        sendToClient(emitter, "connect", "connected!");

        return emitter;
    }

    /**
     * 특정 사용자에게 구축된 파이프라인을 통해 실시간 이벤트를 전송합니다.
     * 전송 실패 시 해당 사용자의 연결을 관리 목록에서 제거합니다.
     *
     * @param userId 이벤트를 수신할 사용자 ID
     * @param eventName 이벤트 종류 (예: "direct-messages", "notifications")
     * @param data 전송할 데이터 객체
     */
    public void sendToUser(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            String eventId = generateEventId(data);
            sendToClient(emitter, eventId, eventName, data);
        }
    }

    private void sendToClient(SseEmitter emitter, String id, String name, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(id) // 재연결시 LastEventId 파라미터로 사용됨
                    .name(name)
                    .data(data));

        } catch (IOException e) {
            log.warn("SSE 전송 실패 (User disconnected): {}", e.getMessage());
            emitter.complete();
        }
    }

    private void sendToClient(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException e) {
            emitter.complete();
        }
    }

    private String generateEventId(Object data) {
        long timestamp = System.currentTimeMillis();

        if (data instanceof NotificationDto notificationDto) {
            return timestamp + "_NOTI_" + notificationDto.id();
        }

        if (data instanceof DirectMessageDto directMessageDto) {
            return timestamp + "_DM_" + directMessageDto.id();
        }

        return timestamp + "_UNKNOWN_" + 0;
    }
}
