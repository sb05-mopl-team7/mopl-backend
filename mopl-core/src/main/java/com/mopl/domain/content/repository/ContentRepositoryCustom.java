package com.mopl.domain.content.repository;

import com.mopl.domain.content.dto.ContentQueryParams;
import com.mopl.domain.content.entity.Content;

import java.util.List;

public interface ContentRepositoryCustom {
    List<Content> list(ContentQueryParams params);
}
