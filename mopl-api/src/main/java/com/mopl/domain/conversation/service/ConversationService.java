package com.mopl.domain.conversation.service;

import com.mopl.domain.conversation.dto.request.ConversationCreateRequest;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
