package com.mopl.domain.directmessage.service;

import com.mopl.domain.conversation.dto.response.DirectMessageDto;
import com.mopl.domain.conversation.entity.Conversation;
import com.mopl.domain.conversation.entity.DirectMessage;
import com.mopl.domain.conversation.entity.ReadStatus;
import com.mopl.domain.conversation.repository.DirectMessageRepository;
import com.mopl.domain.conversation.repository.ReadStatusRepository;
import com.mopl.domain.user.dto.response.UserSummaryDto;
import com.mopl.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DMChatService {

    private final DirectMessageRepository directMessageRepository;
    private final ReadStatusRepository readStatusRepository;

    @Transactional
    public DirectMessageDto saveMessage(Long senderId, Long conversationId, String content) {
        // 대화방에 참여하고 있는 사용자들의 ReadStatus, 유저 정보, 대화방 정보 fetch join
        List<ReadStatus> participants = readStatusRepository.findAllWithUser(conversationId);

        ReadStatus myStatus = null;
        User receiver = null;

        for (ReadStatus status : participants) {
            if (status.getUser().getId().equals(senderId)) {
                myStatus = status;
            } else {
                receiver = status.getUser();
            }
        }

        if (myStatus == null) {
            throw new IllegalArgumentException("대화방에 참여하고 있지 않습니다.");
        }
        if (receiver == null) {
            throw new IllegalArgumentException("상대방을 찾을 수 없습니다.");
        }

        User sender = myStatus.getUser();
        Conversation conversation = myStatus.getConversation();

        DirectMessage message = new DirectMessage(conversation, sender, content);
        directMessageRepository.save(message);

        return new DirectMessageDto(
                message.getId(),
                conversationId,
                message.getCreatedAt(),
                new UserSummaryDto(senderId, sender.getName(), sender.getProfileImageUrl()),
                new UserSummaryDto(receiver.getId(), receiver.getName(), receiver.getProfileImageUrl()),
                content
        );
    }
}
