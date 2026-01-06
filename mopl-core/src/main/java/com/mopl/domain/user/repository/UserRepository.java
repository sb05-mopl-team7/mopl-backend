package com.mopl.domain.user.repository;

import com.mopl.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByIdAndLocked(Long userId,boolean locked);
    Optional<User> findByEmail(String email);
}
