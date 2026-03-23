package com.mopl.domain.contentchat.service;

import com.mopl.domain.contentchat.dto.ContentChatDto;
import com.mopl.domain.contentchat.dto.ContentChatSendRequest;
import com.mopl.domain.user.dto.response.UserSummaryDto;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.redis.RedisManager;
import com.mopl.global.redis.RedisNameSpace;
import com.mopl.global.s3.S3Manager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentChatService {

    private final UserRepository userRepository;
    private final S3Manager s3Manager;
    private final RedisManager redisManager;

    public ContentChatDto createMessage(Long userId, ContentChatSendRequest text) {
        // Redis에서 조회 1번
        UserSummaryDto userSummary = redisManager.findByKey(
            RedisNameSpace.USER_SUMMARY,
            userId.toString(),
            UserSummaryDto.class
        ).orElseGet(() -> {
            User user = validateUser(userId);
            String thumbnailUrl = s3Manager.generatePresignedUrl(user.getProfileImageUrl());
            UserSummaryDto summaryDto = new UserSummaryDto(user.getId(), user.getName(), thumbnailUrl);
            redisManager.save(RedisNameSpace.USER_SUMMARY, user.getId().toString(), summaryDto);
            return summaryDto;
        });

        return new ContentChatDto(userSummary, text.content());
    }

    private User validateUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new MoplException(ErrorCode.UNAUTHORIZED));
    }
}
