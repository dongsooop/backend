package com.dongsoop.dongsoop.search.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BoardType {
    PROJECT("PROJECT", "프로젝트"),
    STUDY("STUDY", "스터디"),
    TUTORING("TUTORING", "튜터링"),
    MARKETPLACE("MARKETPLACE", "마켓플레이스"),
    NOTICE("NOTICE", "공지사항"),
    BLINEDATE("BLINEDATE", "과팅");

    private final String code;
    private final String displayName;
}