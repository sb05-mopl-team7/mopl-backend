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
import com.mopl.global.s3.S3Manager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mopl.domain.conversation.exception.ConversationErrorCode.CONVERSATION_ID_MISMATCH;
import static com.mopl.domain.conversation.exception.ConversationErrorCode.CONVERSATION_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DirectMessageService {

    private final DirectMessageRepository directMessageRepository;
    private final ReadStatusRepository readStatusRepository;
    private final S3Manager s3Manager;

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
        String presignedUrl = s3Manager.generatePresignedUrl(targetUser.getProfileImageUrl());
        UserSummaryDto withUserDto = new UserSummaryDto(targetUser.getId(), targetUser.getName(), presignedUrl);

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

    public List<DirectMessageDto> findMissedMessages(Long userId, long lastTimestamp, Long lastId) {
        LocalDateTime lastTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastTimestamp), ZoneId.of("Asia/Seoul"));

        List<DirectMessage> messages = directMessageRepository.findMissedMessages(userId, lastTime, lastId);

        UserSummaryDto mySummaryDto = new UserSummaryDto(userId, "me", null);

        Map<Long, String> urlCache = new HashMap<>(); // key: authorId, value: presignedUrl

        return messages.stream()
                .map(msg -> convertToMissedDto(msg, mySummaryDto, urlCache))
                .toList();
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

    // 받는 사람이 무조건 본인
    private DirectMessageDto convertToMissedDto(DirectMessage message, UserSummaryDto mySummaryDto, Map<Long, String> urlCache) {
        User author = message.getAuthor();

        String presignedUrl = urlCache.computeIfAbsent(
                author.getId(),
                authorId -> s3Manager.generatePresignedUrl(author.getProfileImageUrl())
        );

        // 보낸 사람 (상대방)
        UserSummaryDto senderDto = new UserSummaryDto(
                author.getId(),
                author.getName(),
                presignedUrl
        );

        return new DirectMessageDto(
                message.getId(),
                message.getConversation().getId(),
                message.getCreatedAt(),
                senderDto,   // 보낸 사람 (상대방)
                mySummaryDto, // 받는 사람 (나)
                message.getContent()
        );
    }
}
