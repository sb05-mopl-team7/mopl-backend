package com.mopl.global.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseManager sseManager;

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
        @RequestParam(value = "LastEventId", required = false, defaultValue = "")
        String lastEventId
    ) {
        // TODO: 인증 시스템(JWT) 연동 후 실제 사용자 ID 할당 필요
        Long userId = 1L; // 임시 user
        return sseManager.subscribe(userId);
    }
}
