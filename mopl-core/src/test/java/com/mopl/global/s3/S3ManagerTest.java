package com.mopl.global.s3;

import com.mopl.global.dto.UploadFileRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

// Test 할 땐 @Disabled 주석 처리 후 실행
@Disabled("이 테스트는 실제 S3에 파일을 업로드 합니다. 로컬에서 수동으로만 테스트 해주세요.")
@SpringBootTest(classes = {S3Manager.class, S3Config.class})
@ActiveProfiles("core-test")
class S3ManagerTest {

    @Autowired
    private S3Manager s3Manager;


    @Test
    @DisplayName("S3 파일 업로드 테스트")
    void testUpload() {

        String content = "Hello, MOPL S3 Test!";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(bytes);

        UploadFileRequest request = new UploadFileRequest(
                inputStream,
                "test-file.txt",
                bytes.length,
                "text/plain"
        );

        String savedUrl = s3Manager.upload(request, FileCategory.TEST);
        System.out.println("저장된 URL (DB 저장용): " + savedUrl);

        assertThat(savedUrl).contains("test-file.txt");
        assertThat(savedUrl).contains(FileCategory.TEST.getPath());
    }

    @Test
    @DisplayName("Presigned URL 생성 테스트")
    void testGeneratePresignedUrl() {
        // 1. 테스트용 파일 먼저 업로드
        String content = "Presigned URL Test Content";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        UploadFileRequest request = new UploadFileRequest(
                new ByteArrayInputStream(bytes),
                "presigned-test.txt",
                bytes.length,
                "text/plain"
        );
        String publicUrl = s3Manager.upload(request, FileCategory.TEST);

        // 2. Presigned URL 생성 호출
        String presignedUrl = s3Manager.generatePresignedUrl(publicUrl);

        // 3. 검증
        System.out.println("원본 URL: " + publicUrl);
        System.out.println("생성된 Presigned URL: " + presignedUrl);

        assertThat(presignedUrl).isNotNull();
        assertThat(presignedUrl).contains("X-Amz-Algorithm"); // AWS 서명 알고리즘 포함 여부
        assertThat(presignedUrl).contains("X-Amz-Signature"); // 서명 값 포함 여부
        assertThat(presignedUrl).contains("presigned-test.txt"); // 파일명 포함 여부
    }
}