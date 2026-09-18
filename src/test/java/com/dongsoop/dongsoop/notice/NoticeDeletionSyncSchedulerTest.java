package com.dongsoop.dongsoop.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.service.DepartmentService;
import com.dongsoop.dongsoop.notice.service.NoticeDeletionSyncScheduler;
import com.dongsoop.dongsoop.notice.service.NoticeDeletionSyncService;
import com.dongsoop.dongsoop.notice.service.NoticeDeletionSyncService.SyncResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoticeDeletionSyncSchedulerTest {

    @Mock
    private DepartmentService departmentService;

    @Mock
    private NoticeDeletionSyncService deletionSyncService;

    @Test
    void syncDepartmentsSequentiallyOnSchedulerThread() {
        Department firstDepartment = department(DepartmentType.DEPT_1001);
        Department secondDepartment = department(DepartmentType.DEPT_2001);
        when(departmentService.getAllDepartments())
                .thenReturn(List.of(firstDepartment, secondDepartment));

        Thread schedulerThread = Thread.currentThread();
        List<Thread> executionThreads = new ArrayList<>();
        when(deletionSyncService.sync(firstDepartment)).thenAnswer(invocation -> {
            executionThreads.add(Thread.currentThread());
            return new SyncResult(0, 0);
        });
        when(deletionSyncService.sync(secondDepartment)).thenAnswer(invocation -> {
            executionThreads.add(Thread.currentThread());
            return new SyncResult(0, 0);
        });

        new NoticeDeletionSyncScheduler(departmentService, deletionSyncService)
                .syncDeletedNotices();

        InOrder inOrder = inOrder(deletionSyncService);
        inOrder.verify(deletionSyncService).sync(firstDepartment);
        inOrder.verify(deletionSyncService).sync(secondDepartment);
        assertThat(executionThreads).containsExactly(schedulerThread, schedulerThread);
    }

    private Department department(DepartmentType departmentType) {
        return new Department(departmentType, departmentType.name(), "/notice");
    }
}
