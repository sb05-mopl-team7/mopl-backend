package com.mopl.storage.impl;

import com.mopl.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("dev")
public class LocalFileStorage implements FileStorage {
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${file.base-url:http://localhost:8080/files}")
    private String baseUrl;

    private static final String PROFILE_IMAGE_DIR = "profile-images";

    @Override
    public void deleteFile(String avatarImage) {
        try {
            Path filePath = Paths.get(uploadDir, PROFILE_IMAGE_DIR,avatarImage);
            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                log.info("로컬 파일 삭제 완료: {}", filePath);
            } else {
                log.warn("삭제할 파일이 없음: {}", filePath);
            }
        } catch (Exception e) {
            log.error("로컬 파일 삭제 실패: {}", avatarImage, e);
        }

    }

    @Override
    public String saveFile(MultipartFile avatarImage, long userId) {
        try{
            String originalName = avatarImage.getOriginalFilename();
            String ext = (originalName != null && originalName.contains("."))
                    ? originalName.substring(originalName.lastIndexOf('.'))
                    : "";
            long timestamp = System.currentTimeMillis();
            String fileName = userId+"_"+timestamp+ext;

            Path uploadPath = Paths.get(uploadDir, PROFILE_IMAGE_DIR);
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(avatarImage.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("로컬 파일 저장 완료: {}", filePath);
            return baseUrl + "/" + PROFILE_IMAGE_DIR + "/" + fileName;

        }catch (IOException e){
            log.error("로컬 파일 저장 실패", e);
            throw new RuntimeException("파일 저장 실패: " , e);
        }
    }
}
