package com.mopl.domain.playlist.dto.response;

import com.mopl.global.SortDirection;

import java.util.List;

public record CursorResponsePlaylistDto(
        List<PlaylistDto> data,
        String nextCursor,
        Long nextIdAfter,
        boolean hasNext,
        long totalCount,
        String sortBy,
        SortDirection sortDirection
) {
}
