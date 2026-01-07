package com.mopl.domain.follow.repository;

import com.mopl.domain.follow.entity.Follow;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<@NonNull Follow, @NonNull Long> {

    // 중복 팔로우 체크
    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    // 언팔로우
    void deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    // 팔로워/팔로잉 수 카운트 (마이페이지에 쓰이는것)
    long countByFollowerId(Long followerId);

    long countByFolloweeId(Long followeeId);
}