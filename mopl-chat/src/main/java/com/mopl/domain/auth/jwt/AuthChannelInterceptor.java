package com.mopl.domain.auth.jwt;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthChannelInterceptor implements ChannelInterceptor {

    private final ChatJwtTokenProvider chatJwtTokenProvider;

    @Override
    public @Nullable Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 헤더에서 토큰 꺼내기
            // 프론트에서 Authorization: `Bearer ${accessToken}` 형식으로 보냄
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                if (chatJwtTokenProvider.validateToken(token)) {
                    Authentication authentication = chatJwtTokenProvider.getAuthentication(token);
                    accessor.setUser(authentication);
                } else {
                    throw new MoplException(ErrorCode.INVALID_TOKEN);
                }
            } else {
                throw new MoplException(ErrorCode.TOKEN_NOT_FOUND);
            }
        }
        return message;
    }
}
