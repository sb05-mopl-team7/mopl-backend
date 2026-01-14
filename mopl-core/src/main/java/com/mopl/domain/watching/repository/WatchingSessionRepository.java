package com.mopl.domain.watching.repository;

import com.mopl.domain.watching.entity.WatchingSession;
import org.jspecify.annotations.NonNull;
import org.springframework.data.repository.CrudRepository;

public interface WatchingSessionRepository extends CrudRepository<@NonNull WatchingSession, @NonNull Long> {
}