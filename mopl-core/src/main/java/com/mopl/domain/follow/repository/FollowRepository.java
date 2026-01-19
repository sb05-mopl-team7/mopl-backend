package com.mopl.domain.follow.repository;

import com.mopl.domain.follow.entity.Follow;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FollowRepository extends JpaRepository<@NonNull Follow, @NonNull Long> {

    // 중복 팔로우 체크 및 특정 유저 팔로우 체크
    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    // 팔로워/팔로잉 수 카운트 (마이페이지에 쓰이는것)
    long countByFollowerId(Long followerId);

    long countByFolloweeId(Long followeeId);

    // 특정 유저를 팔로우하는 유저들의 id 조회
    @Query("""
        SELECT f.follower.id FROM Follow f
        WHERE f.followee.id = :followeeId
    """)
    List<Long> findFollowsByFolloweeId(@Param("followeeId") Long followeeId);
}