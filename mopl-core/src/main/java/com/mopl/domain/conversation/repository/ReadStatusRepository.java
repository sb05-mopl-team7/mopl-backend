package com.mopl.domain.conversation.repository;

import com.mopl.domain.conversation.entity.ReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadStatusRepository extends JpaRepository<ReadStatus, Long> {
}
