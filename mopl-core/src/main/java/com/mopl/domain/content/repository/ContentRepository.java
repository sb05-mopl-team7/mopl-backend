package com.mopl.domain.content.repository;

import com.mopl.domain.content.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContentRepository extends JpaRepository<Content, Long>, ContentRepositoryCustom {

    @Query("""
    select c
    from Content c
    left join fetch c.contentTags ct
    left join fetch ct.tag t
    where c.id = :id
""")
    Optional<Content> findByIdWithTags(@Param("id") Long id);

    //playlist 조회 시 tags까지 한 번에 끌고 오기 위한 fetch join (N+1 방지)
    @Query("""
        select distinct c
        from Content c
        left join fetch c.contentTags ct
        left join fetch ct.tag t
        where c.id in :ids
    """)
    List<Content> findAllByIdInWithTags(@Param("ids") Collection<Long> ids);
}