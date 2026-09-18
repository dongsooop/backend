package com.dongsoop.dongsoop.notice.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.notice.dto.CrawledNotice;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import com.dongsoop.dongsoop.notice.util.NoticeCrawl;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NoticeCollectionService {

    private static final int FIRST_PAGE = 1;

    private final NoticeCrawl noticeCrawl;

    /**
     * 첫 페이지에서 아직 저장되지 않은 최신 공지를 수집한다.
     *
     * @param department 수집할 학과
     * @param recentlyNoticeId DB에 저장된 최신 공지 ID
     * @return 새 공지 상세 정보와 학과별 공지 목록
     */
    public CrawledNotice collectNewNotices(Department department, long recentlyNoticeId) {
        Set<NoticeDetails> newNoticeDetails = noticeCrawl.crawlNoticePage(department, FIRST_PAGE).stream()
                .filter(noticeDetails -> noticeDetails.getId() > recentlyNoticeId)
                .collect(Collectors.toSet());
        List<Notice> newNotices = newNoticeDetails.stream()
                .map(noticeDetails -> new Notice(department, noticeDetails))
                .toList();

        log.info("Updated notices for department: {}, new count: {}", department.getId().name(),
                newNoticeDetails.size());

        return new CrawledNotice(newNoticeDetails, newNotices);
    }
}
