package com.mopl.domain.playlist.controller;

import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.playlist.controller.docs.PlaylistControllerDocs;
import com.mopl.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.domain.playlist.dto.response.PlaylistDto;
import com.mopl.domain.playlist.service.PlaylistService;
import com.mopl.global.dto.PageResponse;
import com.mopl.global.enums.SortDirection;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/playlists")
public class PlaylistController implements PlaylistControllerDocs {

    private final PlaylistService playlistService;

    @Override
    @GetMapping
    public ResponseEntity<PageResponse<PlaylistDto>> findAll(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) String keywordLike,
            @RequestParam(required = false) Long ownerIdEqual,
            @RequestParam(required = false) Long subscriberIdEqual,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String idAfter,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) SortDirection sortDirection,
            @RequestParam(required = false) String sortBy
    ) {
        Long requesterId = userPrincipal.getUserId();

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

    @Override
    @PostMapping
    public ResponseEntity<PlaylistDto> create(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody PlaylistCreateRequest request
    ) {
        Long requesterId = userPrincipal.getUserId();
        return ResponseEntity.ok(playlistService.create(requesterId, request));
    }

    @Override
    @PostMapping("/{playlistId}/subscription")
    public ResponseEntity<Void> subscribe(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long playlistId
    ) {
        Long requesterId = userPrincipal.getUserId();
        playlistService.subscribe(requesterId, playlistId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/{playlistId}/subscription")
    public ResponseEntity<Void> unsubscribe(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long playlistId
    ) {
        Long requesterId = userPrincipal.getUserId();
        playlistService.unsubscribe(requesterId, playlistId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/{playlistId}/contents/{contentId}")
    public ResponseEntity<Void> addContent(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long playlistId,
            @PathVariable Long contentId
    ) {
        Long requesterId = userPrincipal.getUserId();
        playlistService.addContent(requesterId, playlistId, contentId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/{playlistId}/contents/{contentId}")
    public ResponseEntity<Void> removeContent(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long playlistId,
            @PathVariable Long contentId
    ) {
        Long requesterId = userPrincipal.getUserId();
        playlistService.removeContent(requesterId, playlistId, contentId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/{playlistId}")
    public ResponseEntity<PlaylistDto> find(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long playlistId
    ) {
        Long requesterId = userPrincipal.getUserId();
        return ResponseEntity.ok(playlistService.find(requesterId, playlistId));
    }

    @Override
    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long playlistId
    ) {
        Long requesterId = userPrincipal.getUserId();
        playlistService.delete(requesterId, playlistId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/{playlistId}")
    public ResponseEntity<PlaylistDto> update(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long playlistId,
            @Valid @RequestBody PlaylistUpdateRequest request
    ) {
        Long requesterId = userPrincipal.getUserId();
        return ResponseEntity.ok(playlistService.update(requesterId, playlistId, request));
    }
}