package com.mopl.global.config;

import com.mopl.domain.auth.jwt.AuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AuthChannelInterceptor authChannelInterceptor;

    @Override
    public void registerStompEndpoints(org.springframework.web.socket.config.annotation.StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") // 클라이언트가 웹소켓 연결을 시작할 엔드포인트
            .setAllowedOrigins(
                    "http://localhost:3000",
                    "http://localhost:8080",
                    "https://d1ocfp6g80vipy.cloudfront.net",
                    "https://mopl-alb-522466110.ap-northeast-2.elb.amazonaws.com",
                    "https://mopl.shop") // TODO application.yml에 옮기기
            .withSockJS(); // SockJS 지원 (웹소켓 미지원 브라우저 대응)
    }

    /** websocket을 사용하는 도메인: 실시간 채팅(chat) DM, 실시간 시청자 */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub"); // 메시지를 구독(수신)하는 접두사
        registry.setApplicationDestinationPrefixes("/pub"); // 메시지를 발행(송신)하는 접두사
    }

    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
         registration.interceptors(authChannelInterceptor);
    }

    @Override
    public void configureClientOutboundChannel(@NonNull ChannelRegistration registration) {
        registration.taskExecutor()
                // 브로커/애플리케이션에서 클라이언트로 나가는 전송은 느린 구독자 영향으로 쉽게 밀릴 수 있어
                // outbound 전용 executor를 분리해 두면 단일 스레드 병목을 줄이고 지연 전파를 완화할 수 있다.
                .corePoolSize(4)
                // 순간적인 fan-out 증가나 특정 세션의 전송 지연이 생길 때 확장할 수 있는 상한선
                .maxPoolSize(16)
                // 무한정 적재하지 않도록 대기열 상한을 두고, 초과 시에는 transport limit과 함께 보호
                .queueCapacity(1000);
    }

    // STOMP WebSocket 전송 속성 지정
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(4 * 8192); // 웹소켓 메시지 하나의 최대 크기
        registry.setTimeToFirstMessage(30000); // 웹소켓 연결 후 클라이언트가 첫번째 메시지(STOMP CONNECT)를 보내기 전까지 기다리는 시간 (ms)
        registry.setSendTimeLimit(10_000); // 느린 클라이언트로 전송이 오래 걸리면 세션 정리를 유도해 전체 outbound 정체를 줄임
        registry.setSendBufferSizeLimit(512 * 1024); // 전송 지연 동안 세션별 버퍼가 무한정 쌓이지 않도록 상한을 둠
    }
}
