package com.mopl.global.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URL;

@Slf4j
@Component
public class ImageDownloadUtil {

    /**
     * URL에서 이미지를 바이트 배열로 다운로드
     * @param imageUrl 다운로드할 이미지 URL
     * @return 이미지 바이트 배열
     */
    public byte[] downloadImage(String imageUrl) {
        try (InputStream in = new URL(imageUrl).openStream()) {
            return in.readAllBytes();
        } catch (Exception e) {
            log.error("이미지 다운로드 실패: {}", imageUrl, e);
            throw new RuntimeException("이미지 다운로드 중 오류가 발생했습니다.", e);
        }
    }

}
