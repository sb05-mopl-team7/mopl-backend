package com.mopl.domain.watching.repository;

import com.mopl.domain.watching.entity.WatchingSession;
import org.jspecify.annotations.NonNull;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface WatchingSessionRepository extends CrudRepository<@NonNull WatchingSession, @NonNull Long> {

    // contentId로 세션 목록을 조회하는 메서드 추가
    List<WatchingSession> findAllByContentId(Long contentId);
}