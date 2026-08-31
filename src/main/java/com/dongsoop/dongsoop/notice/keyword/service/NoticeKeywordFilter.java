package com.dongsoop.dongsoop.notice.keyword.service;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.notice.keyword.entity.NoticeKeyword;
import com.dongsoop.dongsoop.notice.keyword.entity.NoticeKeywordType;
import java.util.List;
import java.util.Map;

/**
 * 기기별 키워드 설정 스냅샷.
 *
 * <p>한 번의 크롤링에서 나온 공지 여러 건에 같은 설정을 재사용하려고 미리 조회해 담아둔다.
 * 공지마다 조회하면 같은 결과를 공지 수만큼 반복해서 읽게 된다.
 *
 * <p>키워드는 기기 단위로 저장되므로 회원/비회원 구분 없이 같은 기준이 적용된다.
 * 로그아웃해 member 참조가 끊긴 기기도 자기 키워드 설정을 그대로 유지한다.
 */
public class NoticeKeywordFilter {

    private final Map<Long, List<NoticeKeyword>> keywordsByDeviceId;

    public NoticeKeywordFilter(Map<Long, List<NoticeKeyword>> keywordsByDeviceId) {
        this.keywordsByDeviceId = keywordsByDeviceId;
    }

    /**
     * 공지 제목에 기기별 키워드 설정을 적용해 발송 대상 기기만 남긴다.
     *
     * @param devices     해당 학과를 구독한 기기 (회원+비회원)
     * @param noticeTitle 공지 제목
     * @return 키워드 조건을 통과한 기기
     */
    public List<MemberDevice> apply(List<MemberDevice> devices, String noticeTitle) {
        return devices.stream()
                .filter(device -> shouldReceiveNotification(keywordsByDeviceId.get(device.getId()), noticeTitle))
                .toList();
    }

    private boolean shouldReceiveNotification(List<NoticeKeyword> keywords, String noticeTitle) {
        if (keywords == null || keywords.isEmpty()) {
            return true;
        }

        if (noticeTitle == null || noticeTitle.isBlank()) {
            return true;
        }

        String titleLower = noticeTitle.toLowerCase();

        List<String> includeKeywords = keywords.stream()
                .filter(kw -> kw.getType() == NoticeKeywordType.INCLUDE)
                .map(kw -> kw.getKeyword().toLowerCase())
                .toList();

        List<String> excludeKeywords = keywords.stream()
                .filter(kw -> kw.getType() == NoticeKeywordType.EXCLUDE)
                .map(kw -> kw.getKeyword().toLowerCase())
                .toList();

        boolean hasExcludeMatch = excludeKeywords.stream().anyMatch(titleLower::contains);
        if (hasExcludeMatch) {
            return false;
        }

        if (!includeKeywords.isEmpty()) {
            return includeKeywords.stream().anyMatch(titleLower::contains);
        }

        return true;
    }
}
