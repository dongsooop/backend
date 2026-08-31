package com.dongsoop.dongsoop.notice.keyword.service;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.notice.keyword.dto.NoticeKeywordRequest;
import com.dongsoop.dongsoop.notice.keyword.dto.NoticeKeywordResponse;
import java.util.List;

public interface NoticeKeywordService {

    // 구버전 API — 인증된 회원의 기기 전체를 대상으로 한다
    List<NoticeKeywordResponse> getKeywordsByMember();

    NoticeKeywordResponse addKeywordByMember(NoticeKeywordRequest request);

    void deleteKeywordByMember(Long keywordId);

    // 신버전 API — 기기 헤더로 지목한 기기 하나를 대상으로 한다
    List<NoticeKeywordResponse> getKeywords(String fid, String deviceToken);

    NoticeKeywordResponse addKeyword(String fid, String deviceToken, NoticeKeywordRequest request);

    void deleteKeyword(String fid, String deviceToken, Long keywordId);

    /**
     * 대상 기기들의 키워드 설정을 한 번에 읽어 필터를 만든다.
     * 반환된 필터는 같은 크롤링에서 나온 공지 여러 건에 재사용한다.
     */
    NoticeKeywordFilter loadFilter(List<MemberDevice> devices);
}
