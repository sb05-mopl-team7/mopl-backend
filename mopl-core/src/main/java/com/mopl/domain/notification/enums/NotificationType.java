package com.mopl.domain.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    // 권한 변경
    ROLE_UPDATED("내 권한이 변경되었어요.", "내 권한이 [%s]에서 [%s]로 변경되었어요.", Level.WARNING),

    // 내 플레이리스트 구독 (body: 플레이리스트 제목)
    PLAYLIST_SUBSCRIBED("%s님이 구독을 시작했습니다.", "%s", Level.INFO),

    // 구독중인 플레이리스트에 콘텐츠 추가됨
    PLAYLIST_CONTENT_ADDED("업데이트 알림", "구독 중인 [%s] 플레이리스트에 콘텐츠가 추가되었어요.", Level.INFO),

    // 팔로우한 사용자의 주요 활동 - 플레이리스트 생성 (body: [플레이리스트 제목] 플레이리스트 설명)
    FOLLOWING_ACTIVITY_PLAYLIST("%s님이 플레이리스트를 만들었어요.", "[%s] %s", Level.INFO),

    // 다른 사용자가 나를 팔로우
    FOLLOW_ME("팔로우 알림", "%s님이 나를 팔로우했어요.", Level.INFO),

    // DM 수신 (title: 발신자 이름, body: 메시지 내용)
    DM_RECEIVED("[DM] %s", "%s", Level.INFO),
    ;

    private final String titlePattern;
    private final String bodyPattern;
    private final Level level;

    public String generateTitle(Object... args) {
        if (titlePattern.contains("%s")) {
            return String.format(titlePattern, args);
        }
        return titlePattern;
    }

    public String generateBody(Object... args) {
        if (bodyPattern.contains("%s")) {
            return String.format(bodyPattern, args);
        }
        return bodyPattern;
    }
}
