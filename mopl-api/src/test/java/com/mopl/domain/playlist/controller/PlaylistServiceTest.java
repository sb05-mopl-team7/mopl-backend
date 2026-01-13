package com.mopl.domain.playlist.controller;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.enums.ContentType;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.domain.playlist.dto.response.PlaylistDto;
import com.mopl.domain.playlist.service.PlaylistService;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.dto.PageResponse;
import com.mopl.global.enums.SortDirection;
import com.mopl.global.exception.MoplException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class PlaylistServiceTest {

    @Autowired private PlaylistService playlistService;
    @Autowired private UserRepository userRepository;
    @Autowired private ContentRepository contentRepository;

    private User owner;
    private User user2;
    private Content content;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(new User("owner", "owner@test.com", "password"));
        user2 = userRepository.save(new User("user2", "user2@test.com", "password"));

        content = contentRepository.save(new Content(
                ContentType.values()[0],
                "테스트 콘텐츠 제목",
                "테스트 콘텐츠 설명",
                "https://example.com/thumb.png"
        ));
    }

    private PlaylistDto createPlaylistAs(Long userId, String title) {
        return playlistService.create(userId, new PlaylistCreateRequest(title, "설명"));
    }

    @Test
    @DisplayName("[기능] 플레이리스트 생성 성공")
    void create_Success() {
        PlaylistDto dto = createPlaylistAs(owner.getId(), "내 플레이리스트");

        assertNotNull(dto);
        assertNotNull(dto.id());
        assertEquals("내 플레이리스트", dto.title());
        assertEquals("설명", dto.description());
        assertNotNull(dto.updatedAt());
        assertEquals(0L, dto.subscriberCount());
        assertTrue(dto.subscribedByMe()); // 소유자는 항상 true
        assertNotNull(dto.contents());
        assertEquals(0, dto.contents().size());
    }

    @Test
    @DisplayName("[기능] 플레이리스트 단건 조회 성공")
    void find_Success() {
        PlaylistDto created = createPlaylistAs(owner.getId(), "조회용");

        PlaylistDto found = playlistService.find(owner.getId(), created.id());

        assertNotNull(found);
        assertEquals(created.id(), found.id());
        assertEquals("조회용", found.title());
        assertTrue(found.subscribedByMe());
    }

    @Test
    @DisplayName("[기능] 플레이리스트 수정 성공")
    void update_Success() {
        PlaylistDto created = createPlaylistAs(owner.getId(), "수정전");

        PlaylistDto updated = playlistService.update(
                owner.getId(),
                created.id(),
                new PlaylistUpdateRequest("수정후", "수정된 설명")
        );

        assertNotNull(updated);
        assertEquals(created.id(), updated.id());
        assertEquals("수정후", updated.title());
        assertEquals("수정된 설명", updated.description());
    }

    @Test
    @DisplayName("[기능] 플레이리스트 삭제 성공")
    void delete_Success() {
        PlaylistDto created = createPlaylistAs(owner.getId(), "삭제용");

        playlistService.delete(owner.getId(), created.id());

        assertThrows(MoplException.class, () -> playlistService.find(owner.getId(), created.id()));
    }

    @Test
    @DisplayName("[기능] 플레이리스트 구독 성공 (subscriberCount 증가)")
    void subscribe_Success() {
        PlaylistDto created = createPlaylistAs(owner.getId(), "구독대상");

        playlistService.subscribe(user2.getId(), created.id());

        PlaylistDto after = playlistService.find(user2.getId(), created.id());
        assertTrue(after.subscribedByMe());
        assertEquals(1L, after.subscriberCount());
    }

    @Test
    @DisplayName("[기능] 플레이리스트 구독 취소 성공 (subscriberCount 감소)")
    void unsubscribe_Success() {
        PlaylistDto created = createPlaylistAs(owner.getId(), "구독취소대상");

        playlistService.subscribe(user2.getId(), created.id());
        playlistService.unsubscribe(user2.getId(), created.id());

        PlaylistDto after = playlistService.find(user2.getId(), created.id());
        assertFalse(after.subscribedByMe());
        assertEquals(0L, after.subscriberCount());
    }

    @Test
    @DisplayName("[기능] 플레이리스트 콘텐츠 추가 성공")
    void addContent_Success() {
        PlaylistDto created = createPlaylistAs(owner.getId(), "콘텐츠추가");

        playlistService.addContent(owner.getId(), created.id(), content.getId());

        PlaylistDto after = playlistService.find(owner.getId(), created.id());
        assertNotNull(after.contents());
        assertEquals(1, after.contents().size());
    }

    @Test
    @DisplayName("[기능] 플레이리스트 콘텐츠 삭제 성공")
    void removeContent_Success() {
        PlaylistDto created = createPlaylistAs(owner.getId(), "콘텐츠삭제");

        playlistService.addContent(owner.getId(), created.id(), content.getId());
        playlistService.removeContent(owner.getId(), created.id(), content.getId());

        PlaylistDto after = playlistService.find(owner.getId(), created.id());
        assertNotNull(after.contents());
        assertEquals(0, after.contents().size());
    }

    @Test
    @DisplayName("[기능] 플레이리스트 목록 조회 성공 (기본 정렬/기본 limit)")
    void findAll_Success() {
        createPlaylistAs(owner.getId(), "목록1");
        createPlaylistAs(owner.getId(), "목록2");
        createPlaylistAs(user2.getId(), "목록3");

        PageResponse<PlaylistDto> page = playlistService.findAll(
                owner.getId(),
                null,   // keywordLike
                null,   // ownerIdEqual
                null,   // subscriberIdEqual
                null,   // cursor
                null,   // idAfter
                10,     // limit
                null,   // sortBy (default updatedAt)
                SortDirection.DESCENDING
        );

        List<PlaylistDto> data = extractData(page);
        assertNotNull(data);
        assertTrue(data.size() >= 3);
    }

    // PageResponse가 record(data())든 class(getData())든 둘 다 대응 (컴파일 안전 + 런타임 안전)
    @SuppressWarnings("unchecked")
    private static <T> List<T> extractData(Object pageResponse) {
        try {
            // record accessor: data()
            Method m = pageResponse.getClass().getMethod("data");
            Object v = m.invoke(pageResponse);
            return (List<T>) v;
        } catch (NoSuchMethodException ignore) {
            // getter: getData()
            try {
                Method m = pageResponse.getClass().getMethod("getData");
                Object v = m.invoke(pageResponse);
                return (List<T>) v;
            } catch (Exception e) {
                // field: data
                try {
                    Field f = pageResponse.getClass().getDeclaredField("data");
                    f.setAccessible(true);
                    Object v = f.get(pageResponse);
                    return (List<T>) v;
                } catch (Exception ex) {
                    throw new AssertionError("PageResponse에서 data 추출 실패 (data()/getData()/field data 모두 없음)", ex);
                }
            }
        } catch (Exception e) {
            throw new AssertionError("PageResponse data() 호출 실패", e);
        }
    }
}
