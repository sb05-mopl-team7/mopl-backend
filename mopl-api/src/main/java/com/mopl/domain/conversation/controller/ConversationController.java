package com.mopl.domain.conversation.controller;

import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.conversation.dto.request.ConversationCreateRequest;
import com.mopl.domain.conversation.dto.request.ConversationSearchCondition;
import com.mopl.domain.conversation.dto.response.ConversationDto;
import com.mopl.domain.conversation.dto.response.ConversationSimpleDto;
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
        Long myId = userPrincipal.getUserId();
        ConversationDto conversationDto = conversationService.createConversation(myId, createRequest);
        return ResponseEntity.ok(conversationDto);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ConversationDto>> findAll(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                 @ModelAttribute @Valid ConversationSearchCondition searchCondition) {

        Long myId = userPrincipal.getUserId();
        PageResponse<ConversationDto> results = conversationService.findMyAllConversations(searchCondition, myId);

        return ResponseEntity.ok(results);
    }

    @PostMapping("/{conversationId}/direct-messages/{directMessageId}/read")
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                           @PathVariable Long conversationId,
                                           @PathVariable Long directMessageId) {

        Long myId = userPrincipal.getUserId();
        conversationService.updateAsRead(myId, conversationId, directMessageId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/with")
    public ResponseEntity<ConversationSimpleDto> getConversationWithUser(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                                         @RequestParam("userId") Long withUserId) {
        Long myId = userPrincipal.getUserId();
        ConversationSimpleDto simpleDto = conversationService.findConversationWithUser(myId, withUserId);
        return ResponseEntity.ok(simpleDto);
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationDto> getConversationById(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                               @PathVariable Long conversationId) {
        Long myId = userPrincipal.getUserId();
        ConversationDto conversationDto = conversationService.findMyConversation(myId, conversationId);
        return ResponseEntity.ok(conversationDto);
    }
}
