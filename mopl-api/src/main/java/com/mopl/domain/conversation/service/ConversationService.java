package com.mopl.domain.conversation.service;

import com.mopl.domain.conversation.dto.ConversationQueryResult;
import com.mopl.domain.conversation.dto.request.ConversationCreateRequest;
import com.mopl.domain.conversation.dto.request.ConversationSearchCondition;
import com.mopl.domain.conversation.dto.response.ConversationDto;
import com.mopl.domain.conversation.dto.response.LastMessage;
import com.mopl.domain.conversation.entity.Conversation;
import com.mopl.domain.conversation.entity.DirectMessage;
import com.mopl.domain.conversation.entity.ReadStatus;
import com.mopl.domain.conversation.exception.ConversationException;
import com.mopl.domain.conversation.repository.ConversationRepository;
import com.mopl.domain.conversation.repository.DirectMessageRepository;
import com.mopl.domain.conversation.repository.ReadStatusRepository;
import com.mopl.domain.user.dto.response.UserSummaryDto;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.exception.UserErrorCode;
import com.mopl.domain.user.exception.UserException;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.mopl.domain.conversation.exception.ConversationErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ReadStatusRepository readStatusRepository;
    private final DirectMessageRepository directMessageRepository;

    @Transactional
    public ConversationDto createConversation(Long myId, ConversationCreateRequest createRequest) {
        Long withUserId = createRequest.withUserId();
        if (myId.equals(withUserId)) {
            throw new ConversationException(SELF_CONVERSATION_NOT_ALLOWED);
        }

        User me = getUserOrThrow(myId);
        User targetUser = getUserOrThrow(withUserId);

        return conversationRepository.findByParticipants(myId, withUserId)
                .map(conversation -> fetchDetailsAndConvertToDto(conversation, me.getId(), targetUser))
                .orElseGet(() -> createNewConversation(me, targetUser));
    }

    // 기존 대화방이 없을 때 새로 생성
    private ConversationDto createNewConversation(User me, User target) {

        Conversation newConversation = conversationRepository.save(new Conversation());

        ReadStatus myReadStatus = ReadStatus.create(newConversation, me);
        ReadStatus targetReadStatus = ReadStatus.create(newConversation, target);
        readStatusRepository.saveAll(List.of(myReadStatus, targetReadStatus));

        return convertToDto(newConversation, me.getId(), target, myReadStatus, null);
    }

    /**
     * 추가 정보(채팅방의 마지막 메시지, 나의 읽음 상태) DB 조회 후 DTO로 컨버팅
     */
    private ConversationDto fetchDetailsAndConvertToDto(Conversation conversation, Long myId, User target) {
        DirectMessage lastMessage = directMessageRepository.findTopByConversationIdOrderByCreatedAtDescIdDesc(conversation.getId())
                .orElse(null);

        ReadStatus myReadStatus = readStatusRepository.findByConversationIdAndUserId(conversation.getId(), myId)
                .orElseThrow(() -> new ConversationException(CONVERSATION_NOT_FOUND));

        return convertToDto(conversation, myId, target, myReadStatus, lastMessage);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_EXIST));
    }

    // 대화 목록 전체 조회 (커서 페이지네이션)
    public PageResponse<ConversationDto> findMyAllConversations(ConversationSearchCondition searchCondition, long myId) {
        String keyword = searchCondition.keywordLike();
        LocalDateTime cursor = searchCondition.cursor();
        Long idAfter = searchCondition.idAfter();
        int limit = searchCondition.limit();

        List<ConversationQueryResult> myConversations = conversationRepository.findMyConversations(myId, keyword, cursor, idAfter, limit + 1);

        boolean hasNext = false;
        if (myConversations.size() > limit) {
            hasNext = true;
            myConversations.remove(limit);
        }

        List<ConversationDto> dtos = myConversations.stream()
                .map(tuple -> queryResultToDto(tuple, myId))
                .toList();

        String nextCursor = null;
        String nextIdAfter = null;

        if (hasNext && !dtos.isEmpty()) {
            ConversationDto lastConversation = dtos.get(dtos.size() - 1);

            if (lastConversation.lastestMessage() != null) {
                nextCursor = lastConversation.lastestMessage().createdAt().toString();
            } else {
                ConversationQueryResult lastQueryResult = myConversations.get(myConversations.size() - 1);
                Conversation lastEntity = lastQueryResult.conversation();

                nextCursor = lastEntity.getCreatedAt().toString();
            }

            nextIdAfter = lastConversation.id().toString();
        }

        return PageResponse.<ConversationDto>builder()
                .data(dtos)
                .nextCursor(nextCursor)
                .nextIdAfter(nextIdAfter)
                .hasNext(hasNext)
                .totalCount(0)
                .sortBy(searchCondition.sortBy())
                .sortDirection(searchCondition.sortDirection())
                .build();
    }

    private ConversationDto queryResultToDto(ConversationQueryResult queryResult, Long myId) {
        Conversation conversation = queryResult.conversation();
        User targetUser = queryResult.targetUser();
        DirectMessage message = queryResult.lastMessage();
        ReadStatus myStatus = queryResult.myReadStatus();

        return convertToDto(conversation, myId, targetUser, myStatus, message);
    }

    // 대화방의 메시지 읽음 처리
    @Transactional
    public void updateAsRead(Long myId, Long conversationId, Long directMessageId) {
        ReadStatus myStatus = readStatusRepository.findByConversationIdAndUserId(conversationId, myId)
                .orElseThrow(() -> new ConversationException(CONVERSATION_NOT_FOUND));

        DirectMessage message = directMessageRepository.findByIdAndConversationId(directMessageId, conversationId)
                .orElseThrow(() -> new ConversationException(MESSAGE_NOT_FOUND));

        myStatus.updateLastReadMsg(message);
    }

    private ConversationDto convertToDto(Conversation conversation, Long myId, User targetUser,
                                         ReadStatus myReadStatus, DirectMessage lastMessage) {

        Long conversationId = conversation.getId();
        UserSummaryDto with = new UserSummaryDto(targetUser.getId(), targetUser.getName(), targetUser.getProfileImageUrl());

        if (lastMessage == null) {
            return new ConversationDto(conversationId, with, null, false);
        }

        boolean isSenderMe = lastMessage.getAuthor().getId().equals(myId);
        UserSummaryDto mySummaryDto = new UserSummaryDto(myId, "me", null); // 아이디 외 정보 생략

        UserSummaryDto sender = isSenderMe ? mySummaryDto : with;
        UserSummaryDto receiver = isSenderMe ? with : mySummaryDto;

        LastMessage lastMessageDto = new LastMessage(lastMessage.getId(), conversationId, lastMessage.getCreatedAt(),
                                                     sender, receiver, lastMessage.getContent());

        long myLastReadMsgId = myReadStatus.getLastReadMessage() != null ? myReadStatus.getLastReadMessage().getId() : 0L;
        boolean hasUnread = myLastReadMsgId < lastMessage.getId();

        return new ConversationDto(conversationId, with, lastMessageDto, hasUnread);
    }
}
