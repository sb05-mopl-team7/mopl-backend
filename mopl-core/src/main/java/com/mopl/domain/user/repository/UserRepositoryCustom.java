package com.mopl.domain.user.repository;

import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.enums.Role;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface UserRepositoryCustom {
    List<User> cursorFindAll(
            String keywordLike,
            Role roleEqual,
            Boolean isLocked,
            LocalDateTime cursorCreatedAt,
            Long idAfter,
            Pageable pageable
            );
}
