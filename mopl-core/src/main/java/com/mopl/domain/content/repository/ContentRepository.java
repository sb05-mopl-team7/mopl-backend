package com.mopl.domain.content.repository;

import com.mopl.domain.content.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepository extends JpaRepository<Content, Long>, ContentRepositoryCustom {
}
