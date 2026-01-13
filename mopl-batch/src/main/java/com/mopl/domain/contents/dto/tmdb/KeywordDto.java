package com.mopl.domain.contents.dto.tmdb;

import com.mopl.domain.content.entity.Tag;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record KeywordDto(
        Long id,
        String name
) {

    /**
     * 태그 정보 중복 조회를 방지하기 위한 공유 캐시입니다.
     * - 목적: API 응답 처리 중 반복되는 태그명에 대해 매번 DB 조회를 하지 않고 메모리에서 즉시 반환하여 성능 개선
     * - static: 여러 DTO 인스턴스 및 배치 스텝 간에 캐시를 공유하기 위해 사용
     * - ConcurrentHashMap: 멀티스레드 배치 환경(Parallel Steps)에서도 데이터 안정성을 보장하기 위해 사용
     */
    public static final Map<String, Tag> tagCache = new ConcurrentHashMap<>();
}