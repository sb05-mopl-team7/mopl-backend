package com.mopl.global.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SseHeartbeatScheduler {

    private final SseManager sseManager;

    @Scheduled(fixedRate = 15000)
    public void sendHeartbeat() {
        sseManager.sendHeartbeat();
    }
}
