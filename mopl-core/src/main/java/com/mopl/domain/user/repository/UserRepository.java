package com.mopl.domain.user.repository;

import com.mopl.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}
