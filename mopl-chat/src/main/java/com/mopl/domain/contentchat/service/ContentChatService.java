package com.mopl.domain.contentchat.service;

import com.mopl.domain.contentchat.dto.ContentChatDto;
import com.mopl.domain.contentchat.dto.ContentChatSendRequest;
import com.mopl.domain.user.dto.response.UserSummaryDto;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
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

    public ContentChatDto createMessage(Long userId, ContentChatSendRequest text) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new MoplException(ErrorCode.UNAUTHORIZED));
        String presignedUrl = s3Manager.generatePresignedUrl(user.getProfileImageUrl());

        UserSummaryDto userSummary = new UserSummaryDto(user.getId(), user.getName(), presignedUrl);

        return new ContentChatDto(userSummary, text.content());
    }
}
