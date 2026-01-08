package com.mopl.domain.contentchat.dto;

public record ContentChatDto(
    UserSummary sender,
    String content
) {
}
