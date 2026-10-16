package com.dongsoop.dongsoop.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.exception.DepartmentNotFoundException;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.notice.dto.NoticeListResponse;
import com.dongsoop.dongsoop.notice.dto.NoticeRecentIdByDepartment;
import com.dongsoop.dongsoop.notice.preference.service.NoticePreferenceService;
import com.dongsoop.dongsoop.notice.repository.NoticeRepository;
import com.dongsoop.dongsoop.notice.service.NoticeServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class NoticeSubscriptionContractTest {
    @Mock NoticeRepository notices;
    @Mock DepartmentRepository departments;
    @Mock NoticePreferenceService preferences;
    @InjectMocks NoticeServiceImpl service;
    private final PageRequest page = PageRequest.of(2, 5);
    private final Department department = new Department(DepartmentType.DEPT_2001, "Department", "https://example.test/notice");

    @Test
    void departmentLookupKeepsPageAndRepositoryPayload() {
        Page<NoticeListResponse> result = new PageImpl<>(List.of(mock(NoticeListResponse.class)), page, 11);
        when(departments.findById(DepartmentType.DEPT_2001)).thenReturn(Optional.of(department));
        when(notices.findAllByDepartment(department, page)).thenReturn(result);
        assertThat(service.getNoticeByDepartmentType(DepartmentType.DEPT_2001, page)).isSameAs(result);
    }

    @Test
    void missingDepartmentCannotQueryNotices() {
        assertThatThrownBy(() -> service.getNoticeByDepartmentType(DepartmentType.DEPT_2001, page))
                .isInstanceOf(DepartmentNotFoundException.class);
        verifyNoInteractions(notices);
    }

    @Test
    void unregisteredDeviceReturnsEmptyPageWithoutDepartmentOrNoticeQuery() {
        when(preferences.getDepartmentTypes("fid", "token")).thenThrow(new UnregisteredDeviceException());
        var result = service.getSubscribedNotices("fid", "token", page);
        assertThat(result).isEmpty();
        assertThat(result.getPageable()).isEqualTo(page);
        verifyNoInteractions(departments, notices);
    }

    @Test
    void absentSubscriptionsReturnEmptyPageWithoutStorageQuery() {
        when(preferences.getDepartmentTypes(null, "token")).thenReturn(List.of());
        var result = service.getSubscribedNotices(null, "token", page);
        assertThat(result).isEmpty();
        assertThat(result.getPageable()).isEqualTo(page);
        verifyNoInteractions(departments, notices);
    }

    @Test
    void subscriptionsForRemovedDepartmentsReturnEmptyPage() {
        when(preferences.getDepartmentTypes("fid", null)).thenReturn(List.of(DepartmentType.DEPT_2001));
        when(departments.findAllById(List.of(DepartmentType.DEPT_2001))).thenReturn(List.of());
        assertThat(service.getSubscribedNotices("fid", null, page)).isEmpty();
        verifyNoInteractions(notices);
    }

    @Test
    void validSubscriptionsReturnIntegratedDepartmentResults() {
        var result = Page.<NoticeListResponse>empty(page);
        when(preferences.getDepartmentTypes("fid", "token")).thenReturn(List.of(DepartmentType.DEPT_2001));
        when(departments.findAllById(List.of(DepartmentType.DEPT_2001))).thenReturn(List.of(department));
        when(notices.findAllByDepartmentIn(List.of(department), page)).thenReturn(result);
        assertThat(service.getSubscribedNotices("fid", "token", page)).isSameAs(result);
    }

    @Test
    void latestIdsAreKeyedByDepartmentAndEmptySourceReturnsEmptyMap() {
        var projection = mock(NoticeRecentIdByDepartment.class);
        when(projection.getDepartment()).thenReturn(department);
        when(projection.getRecentId()).thenReturn(19L);
        when(notices.findRecentIdGroupByType()).thenReturn(List.of(projection), List.of());
        var latest = service.getNoticeRecentIdMap();
        assertThat(latest).containsOnlyKeys(department).containsEntry(department, 19L);
        assertThat(service.getNoticeRecentIdMap()).isEmpty();
    }
}
