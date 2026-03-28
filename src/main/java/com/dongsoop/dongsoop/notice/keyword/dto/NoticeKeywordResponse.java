package com.dongsoop.dongsoop.notice.keyword.dto;

import com.dongsoop.dongsoop.notice.keyword.entity.NoticeKeyword;
import com.dongsoop.dongsoop.notice.keyword.entity.NoticeKeywordType;

public record NoticeKeywordResponse(
        Long id,
        String keyword,
        NoticeKeywordType type
) {

    public static NoticeKeywordResponse from(NoticeKeyword noticeKeyword) {
        return new NoticeKeywordResponse(
                noticeKeyword.getId(),
                noticeKeyword.getKeyword(),
                noticeKeyword.getType()
        );
    }
}