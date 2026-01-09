package com.mopl.domain.conversation.controller;

import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.conversation.dto.request.ConversationCreateRequest;
import com.mopl.domain.conversation.dto.request.ConversationSearchCondition;
import com.mopl.domain.conversation.dto.response.ConversationDto;
import com.mopl.domain.conversation.service.ConversationService;
import com.mopl.global.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    public ResponseEntity<ConversationDto> create(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                  @RequestBody @Valid ConversationCreateRequest createRequest) {
        Long userId = userPrincipal.getUserId();
        ConversationDto conversationDto = conversationService.createConversation(userId, createRequest);
        return ResponseEntity.ok(conversationDto);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ConversationDto>> findAll(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                 @ModelAttribute @Valid ConversationSearchCondition searchCondition) {

        Long userId = userPrincipal.getUserId();
        PageResponse<ConversationDto> results = conversationService.findMyAllConversations(searchCondition, userId);

        return ResponseEntity.ok(results);
    }
}
