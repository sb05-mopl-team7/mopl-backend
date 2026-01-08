package com.mopl.domain.playlist.controller;

import com.mopl.domain.playlist.controller.docs.PlaylistControllerDocs;
import com.mopl.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.domain.playlist.dto.response.PlaylistDto;
import com.mopl.domain.playlist.service.PlaylistService;
import com.mopl.global.dto.PageResponse;
import com.mopl.global.enums.SortDirection;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/playlists")
public class PlaylistController implements PlaylistControllerDocs {

    private final PlaylistService playlistService;

    // 1) GET /api/playlists
    @GetMapping
    @Override
    public ResponseEntity<PageResponse<PlaylistDto>> findAll(
            Principal principal,
            @RequestParam(required = false) String keywordLike,
            @RequestParam(required = false) Long ownerIdEqual,
            @RequestParam(required = false) Long subscriberIdEqual,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String idAfter,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit,
            @RequestParam(defaultValue = "DESCENDING") SortDirection sortDirection,
            @RequestParam(defaultValue = "updatedAt") String sortBy
    ) {
        Long requesterId = requireUserId(principal);

        return ResponseEntity.ok(
                playlistService.findAll(
                        requesterId,
                        keywordLike,
                        ownerIdEqual,
                        subscriberIdEqual,
                        cursor,
                        idAfter,
                        limit,
                        sortBy,
                        sortDirection
                )
        );
    }

    // 2) POST /api/playlists
    @PostMapping
    @Override
    public ResponseEntity<PlaylistDto> create(
            Principal principal,
            @RequestBody @Valid PlaylistCreateRequest request
    ) {
        Long requesterId = requireUserId(principal);

        PlaylistDto response = playlistService.create(requesterId, request);
        return ResponseEntity.status(201).body(response);
    }

    // 3) POST /api/playlists/{playlistId}/subscription
    @PostMapping("/{playlistId}/subscription")
    @Override
    public ResponseEntity<Void> subscribe(
            Principal principal,
            @PathVariable Long playlistId
    ) {
        Long requesterId = requireUserId(principal);

        playlistService.subscribe(requesterId, playlistId);
        return ResponseEntity.noContent().build();
    }

    // 4) DELETE /api/playlists/{playlistId}/subscription
    @DeleteMapping("/{playlistId}/subscription")
    @Override
    public ResponseEntity<Void> unsubscribe(
            Principal principal,
            @PathVariable Long playlistId
    ) {
        Long requesterId = requireUserId(principal);

        playlistService.unsubscribe(requesterId, playlistId);
        return ResponseEntity.noContent().build();
    }

    // 5) POST /api/playlists/{playlistId}/contents/{contentId}
    @PostMapping("/{playlistId}/contents/{contentId}")
    @Override
    public ResponseEntity<Void> addContent(
            Principal principal,
            @PathVariable Long playlistId,
            @PathVariable Long contentId
    ) {
        Long requesterId = requireUserId(principal);

        playlistService.addContent(requesterId, playlistId, contentId);
        return ResponseEntity.noContent().build();
    }

    // 6) DELETE /api/playlists/{playlistId}/contents/{contentId}
    @DeleteMapping("/{playlistId}/contents/{contentId}")
    @Override
    public ResponseEntity<Void> removeContent(
            Principal principal,
            @PathVariable Long playlistId,
            @PathVariable Long contentId
    ) {
        Long requesterId = requireUserId(principal);

        playlistService.removeContent(requesterId, playlistId, contentId);
        return ResponseEntity.noContent().build();
    }

    // 7) GET /api/playlists/{playlistId}
    @GetMapping("/{playlistId}")
    @Override
    public ResponseEntity<PlaylistDto> find(
            Principal principal,
            @PathVariable Long playlistId
    ) {
        Long requesterId = requireUserId(principal);

        return ResponseEntity.ok(playlistService.find(requesterId, playlistId));
    }

    // 8) DELETE /api/playlists/{playlistId}
    @DeleteMapping("/{playlistId}")
    @Override
    public ResponseEntity<Void> delete(
            Principal principal,
            @PathVariable Long playlistId
    ) {
        Long requesterId = requireUserId(principal);

        playlistService.delete(requesterId, playlistId);
        return ResponseEntity.noContent().build();
    }

    // 9) PATCH /api/playlists/{playlistId}
    @PatchMapping("/{playlistId}")
    @Override
    public ResponseEntity<PlaylistDto> update(
            Principal principal,
            @PathVariable Long playlistId,
            @RequestBody @Valid PlaylistUpdateRequest request
    ) {
        Long requesterId = requireUserId(principal);

        return ResponseEntity.ok(playlistService.update(requesterId, playlistId, request));
    }

    // JWT 인증 주체에서 userId 꺼내기 (FollowController 방식과 동일)
    private Long requireUserId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new MoplException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            throw new MoplException(ErrorCode.UNAUTHORIZED);
        }
    }
}
