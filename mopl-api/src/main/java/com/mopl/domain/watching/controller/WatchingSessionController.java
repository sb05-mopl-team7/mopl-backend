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

    @Override
    public ResponseEntity<WatchingSessionUserResponse> getWatchingSession(Long watcherId) {
        WatchingSessionUserResponse response = watchingSessionService.getWatchingSession(watcherId);

        // 시청 중인 세션이 없을 경우 204 No Content 반환
        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

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
        // null 방어 및 기본값 설정 (Docs 인터페이스의 defaultValue와 일치)
        int finalLimit = (limit != null) ? limit : 10;
        String finalSortBy = (sortBy != null) ? sortBy : "createdAt";
        SortDirection finalSortDirection = (sortDirection != null) ? sortDirection : SortDirection.DESCENDING;

        WatchingSessionContentListResponse response = watchingSessionService.getWatchingSessionsByContent(
                contentId,
                watcherNameLike,
                cursor,
                idAfter,
                finalLimit,
                finalSortBy,
                finalSortDirection
        );

        return ResponseEntity.ok(response);
    }
}