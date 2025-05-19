package com.dongsoop.dongsoop.notice;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.entity.Notice.NoticeKey;
import com.dongsoop.dongsoop.notice.repository.NoticeDetailsRepository;
import com.dongsoop.dongsoop.notice.repository.NoticeRepository;
import com.dongsoop.dongsoop.notice.service.NoticeScheduler;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
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

    static final Integer MIN_NUMBER_OF_INVOCATIONS = 1;
    @Autowired
    NoticeScheduler noticeScheduler;
    @MockitoBean
    NoticeRepository noticeRepository;
    @MockitoBean
    NoticeDetailsRepository noticeDetailsRepository;
    @MockitoBean
    DepartmentRepository departmentRepository;

    @AfterEach
    void cleanup() {
        noticeRepository.deleteAll();
        noticeDetailsRepository.deleteAll();
    }

    @Test
    void get_at_least_one_notice_from_each_department() throws NoSuchFieldException, SecurityException {
        // given
        List<Department> departmentList = getDepartmentList();

        when(departmentRepository.findAll())
                .thenReturn(departmentList);

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

    List<Department> getDepartmentList() {
        List<Department> departmentList = new ArrayList<>();

        departmentList.add(new Department(DepartmentType.DEPT_1001, "미소속", "/dmu/4904/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_2001, "컴퓨터소프트웨어공학과", "/dmu/4580/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_2002, "인공지능소프트웨어학과", "/dmu/4593/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_2003, "웹응용소프트웨어공학과", "/dmu/4568/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_3001, "기계공학과", "/dmu/4461/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_3002, "기계설계공학과", "/dmu/4474/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_4001, "자동화공학과", "/dmu/4487/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_4002, "로봇소프트웨어과", "/dmu/4502/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_5001, "전기공학과", "/dmu/4518/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_5002, "반도체전자공학과", "/dmu/4530/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_5003, "정보통신공학과", "/dmu/4543/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_5004, "소방안전관리과", "/dmu/4557/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_6001, "생명화학공학과", "/dmu/4605/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_6002, "바이오융합공학과", "/dmu/4617/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_6003, "건축과", "/dmu/4629/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_6004, "실내건축디자인과", "/dmu/4643/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_6005, "시각디자인과", "/dmu/4654/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_6006, "AR·VR콘텐츠디자인과", "/dmu/4666/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_7001, "경영학과", "/dmu/4677/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_7002, "세무회계학과", "/dmu/4687/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_7003, "유통마케팅학과", "/dmu/4697/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_7004, "호텔관광학과", "/dmu/4708/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_7005, "경영정보학과", "/dmu/4719/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_7006, "빅데이터경영과", "/dmu/4729/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_8001, "자유전공학과", "/dmu/4739/subview.do"));
        departmentList.add(new Department(DepartmentType.DEPT_9001, "교양과", "/dmu/4747/subview.do"));

        return departmentList;
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
