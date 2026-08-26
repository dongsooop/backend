package com.dongsoop.dongsoop.notice.keyword.service;

import com.dongsoop.dongsoop.notice.keyword.dto.NoticeKeywordRequest;
import com.dongsoop.dongsoop.notice.keyword.dto.NoticeKeywordResponse;
import java.util.List;

public interface GuestNoticeKeywordService {

    List<NoticeKeywordResponse> getKeywords(String anonymousKey);

    NoticeKeywordResponse addKeyword(String anonymousKey, NoticeKeywordRequest request);

    void deleteKeyword(String anonymousKey, Long keywordId);
}
