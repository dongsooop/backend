package com.dongsoop.dongsoop.notice.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.service.DepartmentService;
import com.dongsoop.dongsoop.notice.service.NoticeDeletionSyncService.SyncResult;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NoticeDeletionSyncScheduler {

    private final DepartmentService departmentService;
    private final NoticeDeletionSyncService deletionSyncService;

    @Value("${notice.thread.count}")
    private int threadCount;

    @Scheduled(cron = "0 0 22 * * *", zone = "Asia/Seoul")
    public void syncDeletedNotices() {
        List<Department> departments = departmentService.getAllDepartments();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        try {
            List<CompletableFuture<Void>> futures = departments.stream()
                    .map(department -> CompletableFuture.runAsync(() -> sync(department), executor))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            executor.shutdown();
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
