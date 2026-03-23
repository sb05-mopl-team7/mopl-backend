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

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentChatService {

    private final UserRepository userRepository;
    private final S3Manager s3Manager;
    private final RedisManager redisManager;

    public ContentChatDto createMessage(Long userId, ContentChatSendRequest text) {

        String userName= getUserName(userId);
        String presignedUrl = getProfileUrl(userId);

        UserSummaryDto userSummary = new UserSummaryDto(userId, userName, presignedUrl);

        return new ContentChatDto(userSummary, text.content());
    }

    private String getUserName(Long userId) {
        Optional<String> userName = redisManager.findByKey(
            RedisNameSpace.USER_NAME,
            userId.toString(),
            String.class
        );

        return userName.orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new MoplException(ErrorCode.UNAUTHORIZED));
            return user.getName();
        });
    }

    /** profile Image presignedUrl 조회 */
    private String getProfileUrl(Long userId) {

        // 1. redis에 있는지 확인
        Optional<String> profileUrl = redisManager.findByKey(
            RedisNameSpace.PROFILE_URL,
            userId.toString(),
            String.class
        );

        // 2. 없다면 새로 발급
        return profileUrl.orElseGet(() -> {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new MoplException(ErrorCode.UNAUTHORIZED));
                return s3Manager.generatePresignedUrl(user.getProfileImageUrl());
            }
        );
    }
}
