package com.mopl.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {
    void deleteFile(String avatarImage);
    String saveFile(MultipartFile avatarImage, long userId);
}
