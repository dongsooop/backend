package com.dongsoop.dongsoop.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import com.dongsoop.dongsoop.notice.repository.NoticeRepository;
import com.dongsoop.dongsoop.notice.service.NoticeDeletionSyncService;
import com.dongsoop.dongsoop.notice.service.NoticeDeletionSyncService.SyncResult;
import com.dongsoop.dongsoop.notice.util.NoticeCrawl;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoticeDeletionSyncServiceTest {

    private static final Department DEPARTMENT = new Department(
            DepartmentType.DEPT_1001,
            "동양공지",
            "/dmu/4904/subview.do"
    );

    @Mock
    private NoticeCrawl noticeCrawl;

    @Mock
    private NoticeRepository noticeRepository;

    private NoticeDeletionSyncService deletionSyncService;

    @BeforeEach
    void setUp() {
        deletionSyncService = new NoticeDeletionSyncService(noticeCrawl, noticeRepository);
    }

    @Test
    @DisplayName("DB에서 최근 공지 12페이지인 120건을 조회한다")
    void fetchRecentTwelvePagesFromDatabase() {
        when(noticeRepository.findRecentNoticesIncludingDeleted(DEPARTMENT, 120))
                .thenReturn(List.of());

        SyncResult result = deletionSyncService.sync(DEPARTMENT);

        assertThat(result.deletedCount()).isZero();
        assertThat(result.restoredCount()).isZero();
        verify(noticeRepository).findRecentNoticesIncludingDeleted(DEPARTMENT, 120);
    }

    @Test
    @DisplayName("사이트에서 사라진 공지는 삭제하고 다시 나타난 공지는 복구한다")
    void syncDeletedAndRestoredNotices() {
        Notice notice110 = notice(110L);
        Notice notice109 = notice(109L);
        Notice notice108 = notice(108L);
        Notice notice107 = notice(107L);
        Notice notice100 = notice(100L);
        notice109.markDeleted(LocalDateTime.of(2026, 9, 17, 22, 0));

        when(noticeRepository.findRecentNoticesIncludingDeleted(eq(DEPARTMENT), anyInt()))
                .thenReturn(List.of(notice110, notice109, notice108, notice107, notice100));
        mockCrawledPages(List.of(List.of(111L, 110L, 109L, 107L, 106L)));

        SyncResult firstResult = deletionSyncService.sync(DEPARTMENT);

        assertThat(firstResult.deletedCount()).isEqualTo(1);
        assertThat(firstResult.restoredCount()).isEqualTo(1);

        mockCrawledPages(List.of(List.of(111L, 110L, 108L, 107L, 106L)));

        SyncResult secondResult = deletionSyncService.sync(DEPARTMENT);

        assertThat(secondResult.deletedCount()).isEqualTo(1);
        assertThat(secondResult.restoredCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("크롤링 범위보다 오래된 DB 공지는 변경하지 않는다")
    void keepNoticeOlderThanCrawledBoundary() {
        Notice notice101 = notice(101L);
        Notice notice100 = notice(100L);

        when(noticeRepository.findRecentNoticesIncludingDeleted(eq(DEPARTMENT), anyInt()))
                .thenReturn(List.of(notice101, notice100));
        mockCrawledPages(List.of(List.of(101L)));

        SyncResult firstResult = deletionSyncService.sync(DEPARTMENT);

        assertThat(firstResult.deletedCount()).isZero();
        assertThat(firstResult.restoredCount()).isZero();

        mockCrawledPages(List.of(List.of(101L, 100L)));

        SyncResult secondResult = deletionSyncService.sync(DEPARTMENT);

        assertThat(secondResult.deletedCount()).isZero();
        assertThat(secondResult.restoredCount()).isZero();
    }

    @Test
    @DisplayName("페이지 간 공지 순서가 바뀌어도 존재하는 공지는 삭제하지 않는다")
    void keepExistingNoticesWhenPageOrderChanges() {
        List<Notice> storedNotices = List.of(
                notice(110L),
                notice(109L),
                notice(108L),
                notice(107L)
        );

        when(noticeRepository.findRecentNoticesIncludingDeleted(eq(DEPARTMENT), anyInt()))
                .thenReturn(storedNotices);
        mockCrawledPages(List.of(
                List.of(110L, 108L),
                List.of(109L, 107L)
        ));

        SyncResult result = deletionSyncService.sync(DEPARTMENT);

        assertThat(result.deletedCount()).isZero();
        assertThat(result.restoredCount()).isZero();
    }

    @Test
    @DisplayName("한 페이지라도 크롤링에 실패하면 공지 상태를 변경하지 않는다")
    void keepStateWhenCrawlingFails() {
        List<Notice> storedNotices = List.of(
                notice(110L),
                notice(109L),
                notice(108L),
                notice(107L)
        );

        when(noticeRepository.findRecentNoticesIncludingDeleted(eq(DEPARTMENT), anyInt()))
                .thenReturn(storedNotices);
        when(noticeCrawl.crawlNoticePage(DEPARTMENT, 1))
                .thenReturn(noticeDetails(List.of(110L, 108L)));
        when(noticeCrawl.crawlNoticePage(DEPARTMENT, 2))
                .thenThrow(new IllegalStateException("crawl failed"));

        assertThatThrownBy(() -> deletionSyncService.sync(DEPARTMENT))
                .isInstanceOf(IllegalStateException.class);

        mockCrawledPages(List.of(
                List.of(110L, 108L),
                List.of(109L, 107L)
        ));

        SyncResult result = deletionSyncService.sync(DEPARTMENT);

        assertThat(result.deletedCount()).isZero();
        assertThat(result.restoredCount()).isZero();
    }

    private void mockCrawledPages(List<List<Long>> pages) {
        when(noticeCrawl.crawlNoticePage(eq(DEPARTMENT), anyInt()))
                .thenAnswer(invocation -> {
                    int page = invocation.getArgument(1);
                    if (page > pages.size()) {
                        return List.of();
                    }
                    return noticeDetails(pages.get(page - 1));
                });
    }

    private List<NoticeDetails> noticeDetails(List<Long> ids) {
        return ids.stream()
                .map(this::noticeDetails)
                .toList();
    }

    private Notice notice(Long id) {
        return new Notice(DEPARTMENT, noticeDetails(id));
    }

    private NoticeDetails noticeDetails(Long id) {
        return new NoticeDetails(
                id,
                "학사지원팀",
                "공지 " + id,
                "/notice/" + id,
                LocalDate.of(2026, 9, 18)
        );
    }
}
