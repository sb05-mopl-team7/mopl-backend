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

    // STOMP WebSocket 전송 속성 지정
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(4 * 8192); // 웹소켓 메시지 하나의 최대 크기
        registry.setTimeToFirstMessage(30000); // 웹소켓 연결 후 클라이언트가 첫번째 메시지(STOMP CONNECT)를 보내기 전까지 기다리는 시간 (ms)
    }
}
