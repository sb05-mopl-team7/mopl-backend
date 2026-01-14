package com.mopl.domain.directmessage.controller;

import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.conversation.dto.response.DirectMessageDto;
import com.mopl.domain.directmessage.dto.request.DirectMessageSendRequest;
import com.mopl.domain.directmessage.service.DMChatService;
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
public class DMChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final DMChatService dmChatService;

    // SEND /pub/conversations/{conversationId}/direct-messages
    @MessageMapping("/conversations/{conversationId}/direct-messages")
    public void getMessage(@DestinationVariable Long conversationId,
                           Authentication authentication,
                           @Payload @Valid DirectMessageSendRequest sendRequest) {

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long myId = userPrincipal.userId();

        DirectMessageDto response = dmChatService.saveMessage(myId, conversationId, sendRequest.content());
        messagingTemplate.convertAndSend("/sub/conversations/" + conversationId + "/direct-messages", response);
    }

}
