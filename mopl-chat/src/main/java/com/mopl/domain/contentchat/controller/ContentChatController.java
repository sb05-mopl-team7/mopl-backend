package com.mopl.domain.contentchat.controller;

import com.mopl.domain.contentchat.dto.ContentChatDto;
import com.mopl.domain.contentchat.dto.ContentChatSendRequest;
import com.mopl.domain.contentchat.dto.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ContentChatController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 서버 - 메시지 전송: /pub/contents/{contentId}/chat
     * 클라이언트 - 메시지 수신: /sub/contents/{contentId}/chat
     */
    @MessageMapping("/contents/{contentId}/chat")
    public void sendMessage(@DestinationVariable Long contentId, ContentChatSendRequest request) {

        // TODO: SecurityContext 또는 세션에서 현재 로그인한 유저 정보(UserSummary)를 가져오는 로직 필요
        // 임시 데이터
        UserSummary sender = new UserSummary(1L, "테스트유저", "https://profile.url");

        ContentChatDto response = new ContentChatDto(sender, request.content());

        //메시지 브로드캐스팅
        messagingTemplate.convertAndSend("/sub/contents/" + contentId + "/chat", response);
    }
}
