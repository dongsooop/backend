package com.dongsoop.dongsoop.notice.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.service.DepartmentService;
import com.dongsoop.dongsoop.notice.service.NoticeDeletionSyncService.SyncResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NoticeDeletionSyncScheduler {

    private final DepartmentService departmentService;
    private final NoticeDeletionSyncService deletionSyncService;

    @Scheduled(cron = "0 0 22 * * *", zone = "Asia/Seoul")
    public void syncDeletedNotices() {
        List<Department> departments = departmentService.getAllDepartments();
        for (Department department : departments) {
            sync(department);
        }
    }

    private void sync(Department department) {
        try {
            SyncResult result = deletionSyncService.sync(department);
            log.info("Notice deletion sync completed. department={}, deleted={}, restored={}",
                    department.getId(), result.deletedCount(), result.restoredCount());
        } catch (Exception exception) {
            log.error("Notice deletion sync failed. department={}", department.getId(), exception);
        }
    }
}
