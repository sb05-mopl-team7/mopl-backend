package com.mopl.domain.contents.repository;

import com.mopl.domain.contents.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentsRepository extends JpaRepository<Content, Long> {
}
