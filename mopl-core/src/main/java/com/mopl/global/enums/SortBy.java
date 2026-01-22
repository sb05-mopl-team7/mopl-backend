package com.mopl.global.enums;

public enum SortBy {
    name("name"),
    email("email"),
    createdAt("createdAt"),
    isLocked("locked"),   // 요청 키는 isLocked 이지만, 실제 프로퍼티명은 locked
    role("role");

    private final String property;

    SortBy(String property) { this.property = property; }
    public String property() { return property; }
}
