package com.mopl.domain.playlist.repository;

import com.mopl.domain.playlist.entity.Playlist;
import com.mopl.global.enums.SortDirection;

import java.time.LocalDateTime;
import java.util.List;

public interface PlaylistRepositoryCustom {

    List<Playlist> cursorFindAll(
            String keywordLike,
            Long ownerIdEqual,
            Long subscriberIdEqual,
            LocalDateTime cursorUpdatedAt,
            Long cursorSubscriberCount,
            Long idAfter,
            int limitPlusOne,
            String sortBy,
            SortDirection sortDirection
    );
}
