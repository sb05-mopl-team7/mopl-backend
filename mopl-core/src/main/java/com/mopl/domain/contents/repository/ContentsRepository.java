package com.mopl.domain.contents.repository;

import com.mopl.domain.contents.entity.Contents;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentsRepository extends JpaRepository<Contents, Long> {
}
