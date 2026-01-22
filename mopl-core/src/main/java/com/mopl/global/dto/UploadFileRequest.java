package com.mopl.global.dto;

import java.io.InputStream;

public record UploadFileRequest(
        InputStream inputStream,
        String originalFileName,
        long fileSize,
        String contentType
) {
}
