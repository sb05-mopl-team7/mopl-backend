package com.mopl.domain.contentchat.dto;

import com.mopl.domain.user.dto.response.UserSummaryDto;

public record ContentChatDto(
    UserSummaryDto sender,
    String content
) {
}
