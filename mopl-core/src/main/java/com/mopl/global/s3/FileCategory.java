package com.mopl.global.s3;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FileCategory {
    CONTENT_THUMBNAIL("contents/thumbnails"),
    PROFILE_IMAGE("profiles"),
    CHAT_FILE("chats"),
    BATCH_LOG("logs/batch"),
    TEST("test");

    private final String path;
}