package com.mopl.domain.content.repository;

import com.mopl.domain.content.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByTag(String name);
}
