package com.mopl.global.s3;

import com.mopl.global.dto.UploadFileRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Manager {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * 파일 업로드
     * @param file 업로드할 파일
     * @param dirName S3 내 폴더 경로 (예: "profile", "chat")
     * @return 저장된 파일의 전체 URL
     */
    public String upload(UploadFileRequest file, FileCategory dirName) {
        String fileName = dirName.getPath() + "/" + UUID.randomUUID() + "_" + file.originalFileName();

        try(java.io.InputStream inputStream = file.inputStream()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(file.contentType())
                    .contentLength(file.fileSize()) // 중요: Content-Length 명시로 스트림 끝 알림
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(inputStream, file.fileSize()));

            return getPublicUrl(fileName);
        } catch (Exception e) {
            log.error("S3 파일 업로드 실패: {}", e.getMessage());
            throw new RuntimeException("S3 파일 업로드 중 오류가 발생했습니다.", e);
        }
    }

    public String uploadByte(byte[] imageBytes, String imageName, FileCategory dirName) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new RuntimeException("업로드할 이미지 데이터가 비어있습니다.");
        }

        String fileName = dirName.getPath() + "/" + UUID.randomUUID() + imageName;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType("image/jpeg")
                    .contentLength((long) imageBytes.length)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(imageBytes));

            log.info("이미지 S3 업로드 완료: {}", fileName);
            return fileName;
        } catch (Exception e) {
            log.error("이미지 S3 업로드 실패: {}", e.getMessage());
            throw new RuntimeException("이미지 S3 업로드 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 파일 삭제
     * @param fileUrl 삭제할 파일의 전체 URL
     */
    public void delete(String fileUrl) {
        try {
            // URL에서 안전한 S3 Key 추출 메서드 사용
            String key = extractKey(fileUrl);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 파일 삭제 완료: {}", key);
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {}", e.getMessage());
        }
    }

    /**
     * 특정 시간 동안만 유효한 조회용 Presigned URL 생성
     * @param fileUrl DB에 저장된 전체 URL 또는 Key
     * @return 서명된 URL
     */
    public String generatePresignedUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return null;

        try {
            // URL에서 안전한 S3 Key 추출 메서드 사용
            String key = extractKey(fileUrl);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10)) // 10분간 유효
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Presigned URL 생성 실패: {}", e.getMessage());
            return fileUrl; // 실패 시 원본 반환 또는 에러 처리
        }
    }

    /** 고정 URL 생성 로직 (DB 저장용) */
    private String getPublicUrl(String fileName) {
        return s3Client.utilities().getUrl(GetUrlRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .build()).toString();
    }

    /** URL에서 S3 Key(파일 경로)를 안전하게 추출하는 메서드 */
    private String extractKey(String fileUrl) {
        try {
            // 1. URL 형식이 아니면(이미 Key라면) 그대로 반환
            if (!fileUrl.startsWith("http")) {
                return fileUrl;
            }

            // 2. URI 파싱을 통해 Path만 추출 (/profile/abc.jpg)
            URI uri = new URI(fileUrl);
            String path = uri.getPath();

            // 3. 맨 앞의 슬래시(/) 제거
            if (path.startsWith("/")) {
                return path.substring(1);
            }
            return path;
        } catch (Exception e) {
            log.error("S3 Key 추출 실패: url={}", fileUrl);
            throw new RuntimeException("잘못된 파일 URL 형식입니다.");
        }
    }
}
