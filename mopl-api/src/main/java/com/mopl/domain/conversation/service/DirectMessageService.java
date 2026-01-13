package com.mopl.domain.conversation.service;

import com.mopl.domain.conversation.dto.request.DMCursorRequest;
import com.mopl.domain.conversation.dto.response.DirectMessageDto;
import com.mopl.domain.conversation.entity.DirectMessage;
import com.mopl.domain.conversation.exception.ConversationException;
import com.mopl.domain.conversation.repository.DirectMessageRepository;
import com.mopl.domain.conversation.repository.ReadStatusRepository;
import com.mopl.domain.user.dto.response.UserSummaryDto;
import com.mopl.domain.user.entity.User;
import com.mopl.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.mopl.domain.conversation.exception.ConversationErrorCode.CONVERSATION_ID_MISMATCH;
import static com.mopl.domain.conversation.exception.ConversationErrorCode.CONVERSATION_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DirectMessageService {

    private final DirectMessageRepository directMessageRepository;
    private final ReadStatusRepository readStatusRepository;

    // 로그인한 사용자의 대화방 메시지 전체 조회
    public PageResponse<DirectMessageDto> findMessages(DMCursorRequest cursorRequest, Long myId, Long conversationId) {
        if (!conversationId.equals(cursorRequest.conversationId())) {
            throw new ConversationException(CONVERSATION_ID_MISMATCH);
        }

        readStatusRepository.findByConversationIdAndUserId(conversationId, myId)
                .orElseThrow(() -> new ConversationException(CONVERSATION_NOT_FOUND));

        User targetUser = readStatusRepository.findPartnerByConversationId(conversationId, myId)
                .orElseThrow(() -> new ConversationException(CONVERSATION_NOT_FOUND));

        int limit = cursorRequest.limit();
        List<DirectMessage> messages = directMessageRepository.findAllMessagesByCursor(
                conversationId,
                cursorRequest.cursor(),
                cursorRequest.idAfter(),
                limit + 1);

        boolean hasNext = false;
        if (messages.size() > limit) {
            hasNext = true;
            messages.remove(limit);
        }

        UserSummaryDto mySummaryDto = new UserSummaryDto(myId, "me", null);
        UserSummaryDto withUserDto = new UserSummaryDto(targetUser.getId(), targetUser.getName(), targetUser.getProfileImageUrl());

        List<DirectMessageDto> messageDtos = messages.stream()
                .map(message -> convertToDto(myId, conversationId, message, mySummaryDto, withUserDto))
                .toList();

        String nextCursor = null;
        String nextIdAfter = null;
        if (hasNext) {
            DirectMessage lastEntity = messages.get(messages.size() - 1);
            nextCursor = String.valueOf(lastEntity.getCreatedAt());
            nextIdAfter = String.valueOf(lastEntity.getId());
        }

        return PageResponse.<DirectMessageDto>builder()
                .data(messageDtos)
                .nextCursor(nextCursor)
                .nextIdAfter(nextIdAfter)
                .hasNext(hasNext)
                .totalCount(0)
                .sortBy(cursorRequest.sortBy())
                .sortDirection(cursorRequest.sortDirection())
                .build();
    }

    private DirectMessageDto convertToDto(Long myId, Long conversationId, DirectMessage message, UserSummaryDto mySummaryDto, UserSummaryDto withUserDto) {
        Long messageId = message.getId();
        LocalDateTime createdAt = message.getCreatedAt();

        boolean isSenderMe = message.getAuthor().getId().equals(myId);
        UserSummaryDto sender = isSenderMe ? mySummaryDto : withUserDto;
        UserSummaryDto receiver = isSenderMe ? withUserDto : mySummaryDto;

        return new DirectMessageDto(
                messageId,
                conversationId,
                createdAt,
                sender,
                receiver,
                message.getContent());
    }
}
