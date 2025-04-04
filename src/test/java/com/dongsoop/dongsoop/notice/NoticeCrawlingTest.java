package com.dongsoop.dongsoop.notice;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

import com.dongsoop.dongsoop.department.Department;
import com.dongsoop.dongsoop.department.DepartmentType;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.entity.Notice.NoticeKey;
import com.dongsoop.dongsoop.notice.repository.NoticeDetailsRepository;
import com.dongsoop.dongsoop.notice.repository.NoticeRepository;
import com.dongsoop.dongsoop.notice.service.NoticeScheduler;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.ReflectionUtils;

@SpringBootTest
class NoticeCrawlingTest {

    @Autowired
    NoticeScheduler noticeScheduler;

    @MockitoBean
    NoticeRepository noticeRepository;

    @MockitoBean
    NoticeDetailsRepository noticeDetailsRepository;

    static final Integer MIN_NUMBER_OF_INVOCATIONS = 1;

    @AfterEach
    void cleanup() {
        noticeRepository.deleteAll();
        noticeDetailsRepository.deleteAll();
    }

    @Test
    void get_at_least_one_notice_from_each_department() throws NoSuchFieldException, SecurityException {
        noticeScheduler.scheduled();

        Field idField = Notice.class.getDeclaredField("id");
        Field departmentField = NoticeKey.class.getDeclaredField("department");

        idField.setAccessible(true);
        departmentField.setAccessible(true);

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

        return departmentSet.size() == DepartmentType.values().length;
    }
}
