package com.dongsoop.dongsoop.notice.keyword.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.notice.keyword.dto.NoticeKeywordRequest;
import com.dongsoop.dongsoop.notice.keyword.dto.NoticeKeywordResponse;
import java.util.List;

public interface NoticeKeywordService {

    NoticeKeywordResponse addKeyword(NoticeKeywordRequest request);

    List<NoticeKeywordResponse> getKeywords();

    void deleteKeyword(Long keywordId);

    List<Member> filterMembersByKeyword(List<Member> members, String noticeTitle);
}
