package com.mopl.global.sse;

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

    /**
     * 클라이언트와 서버 간의 SSE 파이프라인을 생성하고 관리합니다.
     * 생성된 연결은 실시간 채팅, 알림 서비스 등의 단방향 이벤트 전송에 활용됩니다.
     *
     * @param userId 연결을 식별할 사용자 ID
     * @return 실시간 통신을 위한 SseEmitter 객체
     */
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter();
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError((e) -> emitters.remove(userId));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected!"));
        } catch (IOException e) {
            emitters.remove(userId);
        }

        return emitter;
    }

    /**
     * 특정 사용자에게 구축된 파이프라인을 통해 실시간 이벤트를 전송합니다.
     * 전송 실패 시 해당 사용자의 연결을 관리 목록에서 제거합니다.
     *
     * @param userId 이벤트를 수신할 사용자 ID
     * @param eventName 이벤트 종류 (예: "chat", "notification")
     * @param data 전송할 데이터 객체
     */
    public void sendToUser(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                log.error("SSE 전송 실패, userId: {}", userId);
                emitters.remove(userId);
            }
        }
    }
}
