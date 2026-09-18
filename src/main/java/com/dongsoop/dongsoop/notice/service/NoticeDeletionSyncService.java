package com.dongsoop.dongsoop.notice.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import com.dongsoop.dongsoop.notice.repository.NoticeRepository;
import com.dongsoop.dongsoop.notice.util.NoticeCrawl;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoticeDeletionSyncService {

    private static final int CRAWL_PAGE_COUNT = 10;
    private static final int NOTICE_COUNT_PER_PAGE = 10;
    private static final int DB_FETCH_BUFFER_PERCENT = 20;
    private static final int DB_FETCH_LIMIT =
            CRAWL_PAGE_COUNT * NOTICE_COUNT_PER_PAGE * (100 + DB_FETCH_BUFFER_PERCENT) / 100;
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
        long oldestCrawledId = crawledIds.stream()
                .mapToLong(Long::longValue)
                .min()
                .orElseThrow();

        LocalDateTime now = LocalDateTime.now(SEOUL_ZONE);
        List<Notice> changedNotices = new ArrayList<>();
        int deletedCount = 0;
        int restoredCount = 0;

        for (Notice notice : storedNotices) {
            long noticeId = notice.getNoticeDetails().getId();
            if (noticeId < oldestCrawledId) {
                continue;
            }

            boolean changed;
            if (crawledIds.contains(noticeId)) {
                changed = notice.restore();
                if (changed) {
                    restoredCount++;
                }
            } else {
                changed = notice.markDeleted(now);
                if (changed) {
                    deletedCount++;
                }
            }

            if (changed) {
                changedNotices.add(notice);
            }
        }

        if (!changedNotices.isEmpty()) {
            noticeRepository.saveAll(changedNotices);
        }

        return new SyncResult(deletedCount, restoredCount);
    }

    private Set<Long> crawlRecentNoticeIds(Department department) {
        Set<Long> noticeIds = new HashSet<>();

        for (int page = 1; page <= CRAWL_PAGE_COUNT; page++) {
            List<NoticeDetails> notices = noticeCrawl.crawlNoticePage(department, page);
            if (notices.isEmpty()) {
                if (page == 1) {
                    throw new IllegalStateException("공지 목록 첫 페이지가 비어 있습니다.");
                }
                break;
            }

            notices.stream()
                    .map(NoticeDetails::getId)
                    .forEach(noticeIds::add);
        }

        return noticeIds;
    }

    public record SyncResult(
            int deletedCount,
            int restoredCount
    ) {
    }
}
