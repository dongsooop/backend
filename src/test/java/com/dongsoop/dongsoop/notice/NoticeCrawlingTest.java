package com.dongsoop.dongsoop.notice;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.service.DepartmentServiceImpl;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.entity.Notice.NoticeKey;
import com.dongsoop.dongsoop.notice.notification.NoticeNotification;
import com.dongsoop.dongsoop.notice.repository.NoticeDetailsRepository;
import com.dongsoop.dongsoop.notice.repository.NoticeRepository;
import com.dongsoop.dongsoop.notice.service.NoticeSchedulerImpl;
import com.dongsoop.dongsoop.notice.service.NoticeService;
import com.dongsoop.dongsoop.notice.util.NoticeCrawl;
import com.dongsoop.dongsoop.notice.util.NoticeLinkParser;
import com.dongsoop.dongsoop.notice.util.NoticeParser;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

@ExtendWith(MockitoExtension.class)
class NoticeCrawlingTest {

    static final Integer MIN_NUMBER_OF_INVOCATIONS = 1;

    static final List<Department> TEST_DEPARTMENT_LIST = List.of(
            new Department(DepartmentType.DEPT_1001, DepartmentType.DEPT_1001.getName(), "/dmu/4904/subview.do"),
            new Department(DepartmentType.DEPT_2001, DepartmentType.DEPT_2001.getName(), "/dmu/4580/subview.do")
    );

    @Mock
    NoticeRepository noticeRepository;

    @Mock
    NoticeDetailsRepository noticeDetailsRepository;

    @Mock
    DepartmentServiceImpl departmentService;

    @Mock
    NoticeService noticeService;

    NoticeLinkParser noticeLinkParser;

    NoticeParser noticeParser;

    NoticeCrawl noticeCrawl;

    NoticeSchedulerImpl noticeScheduler;

    @Mock
    private NoticeNotification noticeNotification;

    @BeforeEach
    void setUp() throws MalformedURLException {
        this.noticeLinkParser = new NoticeLinkParser();
        ReflectionTestUtils.setField(noticeLinkParser, "layoutHeader", "?");
        ReflectionTestUtils.setField(noticeLinkParser, "departmentNoticeRegex", "'([^' ]*)'");
        ReflectionTestUtils.setField(noticeLinkParser, "departmentUrlPrefix", "/combBbs");
        ReflectionTestUtils.setField(noticeLinkParser, "departmentUrlStart", "javascript");
        ReflectionTestUtils.setField(noticeLinkParser, "departmentUrlSuffix", "/view.do");

        this.noticeParser = new NoticeParser(this.noticeLinkParser, "?");
        this.noticeCrawl = new NoticeCrawl(this.noticeParser);

        ReflectionTestUtils.setField(noticeCrawl, "universityUrl", new URL("https://www.dongyang.ac.kr"));
        ReflectionTestUtils.setField(noticeCrawl, "timeout", 600);
        ReflectionTestUtils.setField(noticeCrawl, "userAgent", "Mozilla/5.0 (Compatible; NoticeBot/1.0)");

        this.noticeScheduler = new NoticeSchedulerImpl(noticeCrawl, noticeRepository,
                noticeDetailsRepository, departmentService, noticeService, noticeNotification);

        ReflectionTestUtils.setField(noticeScheduler, "threadCount", 1);
        ReflectionTestUtils.setField(noticeScheduler, "crawlTimeout", 600);
        ReflectionTestUtils.setField(noticeScheduler, "terminateForceTimeout", 10);
        ReflectionTestUtils.setField(noticeScheduler, "terminateGraceTimeout", 30);
    }

    @Test
    void get_at_least_one_notice_from_each_department() throws NoSuchFieldException, SecurityException {
        // given
        when(departmentService.getAllDepartments())
                .thenReturn(TEST_DEPARTMENT_LIST);

        when(noticeService.getNoticeRecentIdMap())
                .thenReturn(Map.of(
                        TEST_DEPARTMENT_LIST.get(0), 0L,
                        TEST_DEPARTMENT_LIST.get(1), 0L
                ));

        // when
        noticeScheduler.scheduled();

        Field idField = Notice.class.getDeclaredField("id");
        Field departmentField = NoticeKey.class.getDeclaredField("department");

        idField.setAccessible(true);
        departmentField.setAccessible(true);

        // then
        verify(noticeRepository, atLeast(MIN_NUMBER_OF_INVOCATIONS)).saveAll(
                argThat(notices -> validateSavedNoticeByDepartment(notices, idField, departmentField)));
    }

    boolean validateSavedNoticeByDepartment(Iterable<Notice> notices, Field idField, Field departmentField) {
        Set<Department> departmentSet = StreamSupport.stream(notices.spliterator(), false)
                .map(notice -> {
                    NoticeKey noticeKey = (NoticeKey) ReflectionUtils.getField(idField, notice);
                    return (Department) ReflectionUtils.getField(departmentField, noticeKey);
                })
                .collect(Collectors.toSet());

        return departmentSet.size() == TEST_DEPARTMENT_LIST.size();
    }
}
