package com.mopl.global.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import jakarta.annotation.PostConstruct;

@Configuration
@RequiredArgsConstructor
public class WebSocketMetricsConfig {

    private final SimpUserRegistry userRegistry;
    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void init() {
        // SimpUserRegistry에서 현재 인증된 유저 수를 가져와 Prometheus 지표로 등록
        Gauge.builder("spring.websocket.sessions.current", userRegistry,
                        registry -> (double) registry.getUserCount())
                .description("Current number of STOMP WebSocket sessions")
                .register(meterRegistry);
    }
}