package com.mopl.domain.contentchat.controller;

import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.contentchat.dto.ContentChatDto;
import com.mopl.domain.contentchat.dto.ContentChatSendRequest;
import com.mopl.domain.contentchat.service.ContentChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ContentChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ContentChatService contentChatService;

    /**
     * 서버 - 메시지 전송: /pub/contents/{contentId}/chat
     * 클라이언트 - 메시지 수신: /sub/contents/{contentId}/chat
     */
    @MessageMapping("/contents/{contentId}/chat")
    public void sendMessage(
        @DestinationVariable Long contentId,
        Authentication authentication,
        @Payload @Valid ContentChatSendRequest request
    ) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        ContentChatDto response = contentChatService.createMessage(userPrincipal.userId(), request);

        //메시지 브로드캐스팅
        messagingTemplate.convertAndSend("/sub/contents/" + contentId + "/chat", response);
    }
}
