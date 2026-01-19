package com.mopl.domain.directmessage.service;

import com.mopl.domain.conversation.dto.response.DirectMessageDto;
import com.mopl.domain.conversation.entity.Conversation;
import com.mopl.domain.conversation.entity.DirectMessage;
import com.mopl.domain.conversation.entity.ReadStatus;
import com.mopl.domain.conversation.event.DmSendEvent;
import com.mopl.domain.conversation.repository.DirectMessageRepository;
import com.mopl.domain.conversation.repository.ReadStatusRepository;
import com.mopl.domain.notification.enums.NotificationType;
import com.mopl.domain.notification.producer.NotificationEventProducer;
import com.mopl.domain.user.dto.response.UserSummaryDto;
import com.mopl.domain.user.entity.User;
import com.mopl.global.redis.RedisManager;
import com.mopl.global.redis.RedisNameSpace;
import com.mopl.global.s3.S3Manager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DMChatService {

    private final DirectMessageRepository directMessageRepository;
    private final ReadStatusRepository readStatusRepository;
    private final RedisManager redisManager;
    private final NotificationEventProducer notificationEventProducer;
    private final S3Manager s3Manager;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${mopl.kafka.topics.dm}")
    private String dmTopic;

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
            throw new IllegalArgumentException("대화방에 참여하고 있지 않습니다."); // TODO 예외 및 핸들러를 core 모듈로 옮긴 후 도메인 예외로 변경
        }
        if (receiver == null) {
            throw new IllegalArgumentException("상대방을 찾을 수 없습니다.");
        }

        User sender = myStatus.getUser();
        Conversation conversation = myStatus.getConversation();

        DirectMessage message = new DirectMessage(conversation, sender, content);
        directMessageRepository.save(message);

        DirectMessageDto directMessageDto = toDto(senderId, conversationId, content, message, sender, receiver);

        // 상대방 대화창 활성화 여부 확인 후 DM 전송
        boolean isWatching = redisManager.isMember(RedisNameSpace.DM_VIEWERS, String.valueOf(conversationId), String.valueOf(receiver.getId()));
        if (!isWatching) {
            kafkaTemplate.send(dmTopic, new DmSendEvent(
                    receiver.getId(),
                    directMessageDto
            ));

            notificationEventProducer.send(
                    receiver.getId(),
                    NotificationType.DM_RECEIVED,
                    sender.getName(),
                    message.getContent());
        }

        return directMessageDto;
    }

    private DirectMessageDto toDto(Long senderId, Long conversationId, String content, DirectMessage message, User sender, User receiver) {
        String senderProfileUrl = s3Manager.generatePresignedUrl(sender.getProfileImageUrl());
        String receiverProfileUrl = s3Manager.generatePresignedUrl(receiver.getProfileImageUrl());

        return new DirectMessageDto(
                message.getId(),
                conversationId,
                message.getCreatedAt(),
                new UserSummaryDto(senderId, sender.getName(), senderProfileUrl),
                new UserSummaryDto(receiver.getId(), receiver.getName(), receiverProfileUrl),
                content
        );
    }
}
