package com.dongsoop.dongsoop.notice;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.service.DepartmentService;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.ReflectionUtils;

@SpringBootTest(classes = {
        NoticeCrawl.class,
        NoticeLinkParser.class,
        NoticeParser.class,
        NoticeSchedulerImpl.class
})
class NoticeCrawlingTest {

    static final Integer MIN_NUMBER_OF_INVOCATIONS = 1;

    static final List<Department> TEST_DEPARTMENT_LIST = List.of(
            new Department(DepartmentType.DEPT_1001, null, "/dmu/4904/subview.do"),
            new Department(DepartmentType.DEPT_2001, null, "/dmu/4580/subview.do")
    );

    // NoticeSchedulerImpl 의존성 주입
    @MockitoBean
    NoticeRepository noticeRepository;
    @MockitoBean
    NoticeDetailsRepository noticeDetailsRepository;
    @MockitoBean
    DepartmentService departmentService;
    @MockitoBean
    NoticeService noticeService;
    @MockitoBean
    NoticeNotification noticeNotification;

    // NoticeCrawl 의존성 주입
    @Autowired
    NoticeLinkParser noticeLinkParser;
    @Autowired
    NoticeParser noticeParser;

    @Autowired
    NoticeCrawl noticeCrawl;

    @Autowired
    NoticeSchedulerImpl noticeScheduler;

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
