package com.mopl.domain.notification.controller;

import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.global.sse.SseManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(value = "LastEventId", required = false, defaultValue = "")
            String lastEventId
    ) {
        Long userId = userPrincipal.getUserId();

        return sseManager.subscribe(userId);
    }
}
