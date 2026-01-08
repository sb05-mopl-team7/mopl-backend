package com.mopl.domain.conversation.controller;

import com.mopl.domain.conversation.dto.request.ConversationCreateRequest;
import com.mopl.domain.conversation.dto.response.ConversationDto;
import com.mopl.domain.conversation.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    // TODO 회원가입/로그인 완료 후 AuthenticationPrincipal 로 변경 필요
    @PostMapping
    public ResponseEntity<ConversationDto> create(@RequestBody @Valid ConversationCreateRequest createRequest) {
        long tempUserId = 1L;
        ConversationDto conversationDto = conversationService.createConversation(tempUserId, createRequest);
        return ResponseEntity.ok(conversationDto);
    }
}
