package com.mopl.global.config;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // TODO: JWT 인증 인터셉터 구현 - private final StompHandler stompHandler;

    @Override
    public void registerStompEndpoints(org.springframework.web.socket.config.annotation.StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") // 클라이언트가 웹소켓 연결을 시작할 엔드포인트
            .setAllowedOrigins("http://localhost:3000") // 운영 시 실제 도메인 넣어야 함
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
        // TODO: 메시지가 들어오는 통로에 인터셉터를 등록하여 JWT 인증 등을 처리
        // registration.interceptors(stompHandler);
    }
}
