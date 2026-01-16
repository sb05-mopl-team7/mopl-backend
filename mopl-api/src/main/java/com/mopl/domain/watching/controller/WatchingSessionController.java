package com.mopl.domain.watching.controller;

import com.mopl.domain.watching.controller.docs.WatchingSessionControllerDocs;
import com.mopl.domain.watching.dto.response.WatchingSessionContentListResponse;
import com.mopl.domain.watching.dto.response.WatchingSessionUserResponse;
import com.mopl.domain.watching.service.WatchingSessionService;
import com.mopl.global.enums.SortDirection;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WatchingSessionController implements WatchingSessionControllerDocs {

    private final WatchingSessionService watchingSessionService;

    // 특정 사용자의 시청 세션 단건 조회
    @Override
    public ResponseEntity<WatchingSessionUserResponse> getWatchingSession(Long watcherId) {
        WatchingSessionUserResponse response = watchingSessionService.getWatchingSession(watcherId);
        return ResponseEntity.ok(response);
    }

    // 특정 콘텐츠의 시청 세션 목록 조회 (커서 페이지네이션)
    @Override
    public ResponseEntity<WatchingSessionContentListResponse> getWatchingSessionsByContent(
            Long contentId,
            String watcherNameLike,
            String cursor,
            Long idAfter,
            Integer limit,
            String sortBy,
            SortDirection sortDirection
    ) {
        WatchingSessionContentListResponse response = watchingSessionService.getWatchingSessionsByContent(
                contentId,
                watcherNameLike,
                cursor,
                idAfter,
                limit,
                sortBy,
                sortDirection
        );
        return ResponseEntity.ok(response);
    }
}