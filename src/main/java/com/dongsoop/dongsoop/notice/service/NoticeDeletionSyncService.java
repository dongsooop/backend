package com.dongsoop.dongsoop.notice.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import com.dongsoop.dongsoop.notice.repository.NoticeRepository;
import com.dongsoop.dongsoop.notice.util.NoticeCrawl;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoticeDeletionSyncService {

    private static final int CRAWL_PAGE_COUNT = 10;
    private static final int NOTICE_COUNT_PER_PAGE = 10;
    private static final int DB_FETCH_PAGE_COUNT = 12;
    private static final int DB_FETCH_LIMIT = DB_FETCH_PAGE_COUNT * NOTICE_COUNT_PER_PAGE;
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final NoticeCrawl noticeCrawl;
    private final NoticeRepository noticeRepository;

    public SyncResult sync(Department department) {
        List<Notice> storedNotices = noticeRepository.findRecentNoticesIncludingDeleted(
                department,
                DB_FETCH_LIMIT
        );
        if (storedNotices.isEmpty()) {
            return new SyncResult(0, 0);
        }

        Set<Long> crawledIds = crawlRecentNoticeIds(department);
        Map<Boolean, List<Notice>> noticesByRemaining = partitionByRemaining(storedNotices, crawledIds);

        // markDeleted 와 restore 는 실제로 상태가 바뀐 경우에만 true 를 반환한다
        LocalDateTime deletedAt = LocalDateTime.now(SEOUL_ZONE);
        List<Notice> deletedNotices = noticesByRemaining.get(false).stream()
                .filter(notice -> notice.markDeleted(deletedAt))
                .toList();
        List<Notice> restoredNotices = noticesByRemaining.get(true).stream()
                .filter(Notice::restore)
                .toList();

        noticeRepository.saveAll(Stream.concat(deletedNotices.stream(), restoredNotices.stream()).toList());

        return new SyncResult(deletedNotices.size(), restoredNotices.size());
    }

    /**
     * 크롤링 범위 안의 공지를 사이트에 남아 있는 것(true)과 사라진 것(false)으로 가른다. 상태는 바꾸지 않는다.
     */
    private Map<Boolean, List<Notice>> partitionByRemaining(List<Notice> storedNotices, Set<Long> crawledIds) {
        long oldestCrawledId = Collections.min(crawledIds);

        return storedNotices.stream()
                .filter(notice -> notice.getNoticeDetails().getId() >= oldestCrawledId)
                .collect(Collectors.partitioningBy(
                        notice -> crawledIds.contains(notice.getNoticeDetails().getId())));
    }

    private Set<Long> crawlRecentNoticeIds(Department department) {
        Set<Long> noticeIds = new HashSet<>();

        for (int page = 1; page <= CRAWL_PAGE_COUNT; page++) {
            List<NoticeDetails> notices = noticeCrawl.crawlNoticePage(department, page);
            if (notices.isEmpty()) {
                break;
            }

            notices.forEach(noticeDetails -> noticeIds.add(noticeDetails.getId()));
        }

        if (noticeIds.isEmpty()) {
            throw new IllegalStateException("공지 목록 첫 페이지가 비어 있습니다.");
        }

        return noticeIds;
    }

    public record SyncResult(
            int deletedCount,
            int restoredCount
    ) {
    }
}
