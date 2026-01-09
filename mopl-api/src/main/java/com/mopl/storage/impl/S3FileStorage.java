package com.mopl.storage.impl;

import com.mopl.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("prod") //TODO : 아직 연결이 안되어 있어서 테스트 나중에 해야 함
public class S3FileStorage implements FileStorage {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    private static final String PROFILE_IMAGE_DIR = "profile-images";

    @Override
    public void deleteFile(String avatarImage) {
        try {
            String key = PROFILE_IMAGE_DIR + "/" + avatarImage;
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 파일 삭제 완료: {}", key);
        }catch (NoSuchKeyException e) {
            log.warn("삭제할 파일이 없음: {}", avatarImage);
        }  catch (S3Exception e) {
            log.error("S3 파일 삭제 실패: {}", avatarImage, e);
        }
    }

    @Override
    public String saveFile(MultipartFile avatarImage, long userId) {
        try {
            String originalName = avatarImage.getOriginalFilename();
            String ext = (originalName != null && originalName.contains("."))
                    ? originalName.substring(originalName.lastIndexOf('.'))
                    : "";
            long timestamp = System.currentTimeMillis();
            String fileName = userId + "_" + timestamp + ext;

            //전체 경로
            String key = PROFILE_IMAGE_DIR + "/" + fileName;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(avatarImage.getContentType())
                    .acl(ObjectCannedACL.PUBLIC_READ)  // 공개 읽기
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(avatarImage.getInputStream(), avatarImage.getSize()));

            String url = String.format("https://%s.s3.%s.amazonaws.com/%s",
                    bucketName, region, key);

            log.info("S3 파일 저장 완료: {}", key);
            return url;

        } catch (IOException e) {
            log.error("S3 파일 저장 실패", e);
            throw new RuntimeException("파일 저장 실패", e);
        } catch (S3Exception e) {
            log.error("S3 서비스 오류", e);
            throw new RuntimeException("S3 저장 실패", e);
        }
    }
}
