package com.dongsoop.dongsoop.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.notice.dto.CrawledNotice;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import com.dongsoop.dongsoop.notice.service.NoticeCollectionService;
import com.dongsoop.dongsoop.notice.util.NoticeCrawl;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoticeCollectionServiceTest {

    private static final Department DEPARTMENT =
            new Department(DepartmentType.DEPT_1001, null, "/notice");

    @Mock
    private NoticeCrawl noticeCrawl;

    @InjectMocks
    private NoticeCollectionService noticeCollectionService;

    @Test
    void collectOnlyNoticesNewerThanRecentlyStoredNotice() {
        NoticeDetails oldNotice = noticeDetails(100L);
        NoticeDetails firstNewNotice = noticeDetails(101L);
        NoticeDetails secondNewNotice = noticeDetails(102L);
        when(noticeCrawl.crawlNoticePage(DEPARTMENT, 1))
                .thenReturn(List.of(secondNewNotice, firstNewNotice, oldNotice));

        CrawledNotice result = noticeCollectionService.collectNewNotices(DEPARTMENT, 100L);

        assertThat(result.getNoticeDetailSet())
                .extracting(NoticeDetails::getId)
                .containsExactlyInAnyOrder(101L, 102L);
        assertThat(result.getNoticeList())
                .extracting(Notice::getDepartment)
                .containsOnly(DEPARTMENT);
        assertThat(result.getNoticeList())
                .extracting(notice -> notice.getNoticeDetails().getId())
                .containsExactlyInAnyOrder(101L, 102L);
        verify(noticeCrawl).crawlNoticePage(DEPARTMENT, 1);
    }

    @Test
    void returnEmptyResultWhenPageHasNoNewNotice() {
        when(noticeCrawl.crawlNoticePage(DEPARTMENT, 1))
                .thenReturn(List.of(noticeDetails(99L), noticeDetails(100L)));

        CrawledNotice result = noticeCollectionService.collectNewNotices(DEPARTMENT, 100L);

        assertThat(result.getNoticeDetailSet()).isEmpty();
        assertThat(result.getNoticeList()).isEmpty();
    }

    private NoticeDetails noticeDetails(long id) {
        return new NoticeDetails(id, "writer", "title", "/notice/" + id, LocalDate.of(2026, 9, 18));
    }
}
