package com.mopl.domain.watching.controller;

import com.mopl.domain.watching.controller.docs.WatchingSessionControllerDocs;
import com.mopl.domain.watching.dto.response.WatchingSessionResponse;
import com.mopl.domain.watching.service.WatchingSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WatchingSessionController implements WatchingSessionControllerDocs {

    private final WatchingSessionService watchingSessionService;

    @Override
    public ResponseEntity<WatchingSessionResponse> getWatchingSession(Long watcherId) {
        WatchingSessionResponse response = watchingSessionService.getWatchingSession(watcherId);
        return ResponseEntity.ok(response);
    }
}