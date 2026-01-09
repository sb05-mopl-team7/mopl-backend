package com.mopl.domain.conversation.service;

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
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.mopl.domain.conversation.exception.ConversationErrorCode.CONVERSATION_NOT_FOUND;
import static com.mopl.domain.conversation.exception.ConversationErrorCode.SELF_CONVERSATION_NOT_ALLOWED;

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
    public ConversationDto createConversation(Long userId, ConversationCreateRequest createRequest) {
        Long withUserId = createRequest.withUserId();
        if (userId.equals(withUserId)) {
            throw new ConversationException(SELF_CONVERSATION_NOT_ALLOWED);
        }

        User me = getUserOrThrow(userId);
        User targetUser = getUserOrThrow(withUserId);

        return conversationRepository.findByParticipants(userId, withUserId)
                .map(conversation -> convertToDtoWithTarget(conversation, me, targetUser))
                .orElseGet(() -> createNewConversation(me, targetUser));
    }

    // 기존 대화방이 없을 때 새로 생성
    private ConversationDto createNewConversation(User me, User target) {

        Conversation newConversation = conversationRepository.save(new Conversation());

        ReadStatus myReadStatus = ReadStatus.create(newConversation, me);
        ReadStatus targetReadStatus = ReadStatus.create(newConversation, target);
        readStatusRepository.saveAll(List.of(myReadStatus, targetReadStatus));

        UserSummaryDto targetUserDto = new UserSummaryDto(target.getId(), target.getName(), target.getProfileImageUrl());

        return new ConversationDto(
                newConversation.getId(),
                targetUserDto,
                null,
                false
        );
    }

    private ConversationDto convertToDtoWithTarget(Conversation conversation, User me, User target) {
        UserSummaryDto withUserDto = new UserSummaryDto(target.getId(), target.getName(), target.getProfileImageUrl());

        DirectMessage lastMessage = directMessageRepository.findTopByConversationIdOrderByCreatedAtDescIdDesc(conversation.getId())
                .orElse(null);

        if (lastMessage == null) {
            return new ConversationDto(conversation.getId(),
                    withUserDto,
                    null,
                    false);
        }

        User sender = lastMessage.getAuthor();
        UserSummaryDto senderDto = null;
        UserSummaryDto receiverDto = null;

        if (sender.getId().equals(me.getId())) {
            senderDto = new UserSummaryDto(me.getId(),
                    me.getName(),
                    me.getProfileImageUrl());
            receiverDto = new UserSummaryDto(target.getId(), target.getName(), target.getProfileImageUrl());
        } else {
            senderDto = new UserSummaryDto(target.getId(), target.getName(), target.getProfileImageUrl());
            receiverDto = new UserSummaryDto(me.getId(),
                    me.getName(),
                    me.getProfileImageUrl());
        }

        LastMessage lastMessageDto = new LastMessage(lastMessage.getId(),
                conversation.getId(),
                lastMessage.getCreatedAt(),
                senderDto,
                receiverDto,
                lastMessage.getContent());

        ReadStatus myReadStatus = readStatusRepository.findByConversationIdAndUserId(conversation.getId(), me.getId())
                .orElseThrow(() -> new ConversationException(CONVERSATION_NOT_FOUND));

        long lastReadMsgId = myReadStatus.getLastReadMessage() != null ? myReadStatus.getLastReadMessage().getId() : 0L;
        boolean hasUnread = lastReadMsgId < lastMessage.getId();

        return new ConversationDto(conversation.getId(), withUserDto, lastMessageDto, hasUnread);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_EXIST));
    }

    // 대화 목록 전체 조회 (커서 페이지네이션)
    public PageResponse<ConversationDto> findMyAllConversations(ConversationSearchCondition searchCondition, long userId) {
        String keyword = searchCondition.keywordLike();
        LocalDateTime cursor = searchCondition.cursor();
        Long idAfter = searchCondition.idAfter();
        int limit = searchCondition.limit();

        List<Tuple> myConversations = conversationRepository.findMyConversations(userId, keyword, cursor, idAfter, limit + 1);

        boolean hasNext = false;
        if (myConversations.size() > limit) {
            hasNext = true;
            myConversations.remove(limit);
        }

        List<ConversationDto> dtos = myConversations.stream()
                .map(tuple -> convertToDto(tuple, userId))
                .toList();

        String nextCursor = null;
        String nextIdAfter = null;

        if (hasNext && !dtos.isEmpty()) {
            ConversationDto lastConversation = dtos.get(dtos.size() - 1);

            if (lastConversation.lastestMessage() != null) {
                nextCursor = lastConversation.lastestMessage().createdAt().toString();
            } else {
                Tuple lastTuple = myConversations.get(myConversations.size() - 1);
                Conversation lastEntity = lastTuple.get(0, Conversation.class);

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

    private ConversationDto convertToDto(Tuple tuple, Long userId) {
        Conversation conversation = tuple.get(0, Conversation.class);
        User targetUser = tuple.get(1, User.class);
        DirectMessage message = tuple.get(2, DirectMessage.class);
        ReadStatus myStatus = tuple.get(3, ReadStatus.class);

        UserSummaryDto withUserDto = new UserSummaryDto(targetUser.getId(), targetUser.getName(), targetUser.getProfileImageUrl());

        if (message == null) {
            return new ConversationDto(conversation.getId(), withUserDto, null, false);
        }

        boolean isSenderMe = message.getAuthor().getId().equals(userId);
        UserSummaryDto meSummaryDto = new UserSummaryDto(userId, "me", null); // 아이디 외 정보 생략

        UserSummaryDto sender = isSenderMe ? meSummaryDto : withUserDto;
        UserSummaryDto receiver = isSenderMe ? withUserDto : meSummaryDto;

        LastMessage lastMessageDto = new LastMessage(
                message.getId(),
                conversation.getId(),
                message.getCreatedAt(),
                sender,
                receiver,
                message.getContent()
        );

        long lastReadMsgId = myStatus.getLastReadMessage() != null ? myStatus.getLastReadMessage().getId() : 0L;
        boolean hasUnread = lastReadMsgId < message.getId();

        return new ConversationDto(conversation.getId(), withUserDto, lastMessageDto, hasUnread);
    }
}
