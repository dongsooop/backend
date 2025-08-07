package com.dongsoop.dongsoop.notice.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.notice.dto.CrawledNotice;
import com.dongsoop.dongsoop.notice.dto.NoticeRecentIdByDepartment;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import com.dongsoop.dongsoop.notice.repository.NoticeDetailsRepository;
import com.dongsoop.dongsoop.notice.repository.NoticeRepository;
import com.dongsoop.dongsoop.notice.util.NoticeCrawl;
import com.dongsoop.dongsoop.notification.service.NotificationService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NoticeSchedulerImpl implements NoticeScheduler {

    private final NoticeCrawl noticeCrawl;
    private final NoticeRepository noticeRepository;
    private final NoticeDetailsRepository noticeDetailsRepository;
    private final DepartmentRepository departmentRepository;
    private final NotificationService notificationService;

    @Value("${notice.thread.count}")
    private int threadCount;

    @Value("${notice.crawl.timeout}")
    private int crawlTimeout;

    @Value("${notice.terminate.force-time}")
    private int terminateForceTimeout;

    @Value("${notice.terminate.grace-time}")
    private int terminateGraceTimeout;

    @Override
    @Scheduled(cron = "0 0 10,14,18 * * *", zone = "Asia/Seoul")
    public void scheduled() {
        log.info("notice crawling scheduler started");
        // 학과별 최신 공지 번호(가장 높은 번호) 가져오기
        List<NoticeRecentIdByDepartment> noticeRecentIdByDepartmentList = noticeRepository.findRecentIdGroupByType();
        Map<Department, Long> noticeRecentIdMap = transformNoticeRecentIdListToMap(noticeRecentIdByDepartmentList);

        // 학과 전체 가져오기
        List<Department> departmentList = departmentRepository.findAll();

        // 크롤링 멀티 스레딩 처리
        multiThreading(departmentList, noticeRecentIdMap);

        log.info("notice crawling scheduler ended");
    }

    /**
     * 학과별 공지 크롤링에 대한 멀티 스레딩 처리
     *
     * @param departmentList 학과 목록
     * @param noticeMaxIdMap 학과별 최신 공지 ID 맵
     */
    private void multiThreading(List<Department> departmentList,
                                Map<Department, Long> noticeMaxIdMap) {
        // 저장할 최신 공지사항 리스트 변수 초기화
        Set<Notice> noticeSet = ConcurrentHashMap.newKeySet();
        Set<NoticeDetails> noticeDetailSet = ConcurrentHashMap.newKeySet();

        // ExecutorService 로 스레드 풀 관리
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        try {
            List<CompletableFuture<Void>> futures = departmentList.stream()
                    .map(department -> CompletableFuture.runAsync(
                            () -> crawl(noticeMaxIdMap, department, noticeSet, noticeDetailSet)
                            , executor))
                    .toList();

            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );

            allFutures.get(crawlTimeout, TimeUnit.SECONDS);
            saveResults(noticeDetailSet, noticeSet, departmentList.size());
        } catch (InterruptedException exception) {
            log.error("Notice crawling interrupted", exception);
            Thread.currentThread().interrupt();
        } catch (TimeoutException exception) {
            log.error("Notice crawling timed out", exception);
            log.warn("Notice crawling did not complete within the timeout period, saving partial results");
            saveResults(noticeDetailSet, noticeSet, departmentList.size());
        } catch (Exception exception) {
            log.error("Notice crawling failed", exception);
        } finally {
            shutdownExecutor(executor);
        }
    }

    /**
     * 학과별 공지 크롤링
     *
     * @param noticeMaxIdMap  학과별 최신 공지 ID 맵
     * @param department      학과 정보
     * @param noticeSet       학과별 공지사항을 저장할 Set
     * @param noticeDetailSet 학과별 공지사항 상세 정보를 저장할 Set
     */
    private void crawl(Map<Department, Long> noticeMaxIdMap, Department department,
                       Set<Notice> noticeSet, Set<NoticeDetails> noticeDetailSet) {
        // 학과 최신 공지 번호(가장 높은 번호)
        Long recentlyNoticeId = noticeMaxIdMap.getOrDefault(department, 0L);

        CrawledNotice crawledNotice = noticeCrawl.crawlNewNotices(department, recentlyNoticeId);
        noticeSet.addAll(crawledNotice.getNoticeList());
        noticeDetailSet.addAll(crawledNotice.getNoticeDetailSet());

        if (!crawledNotice.getNoticeDetailSet().isEmpty()) {
            notificationService.sendNotificationByDepartment(department, crawledNotice.getNoticeDetailSet());
        }
    }

    /**
     * ExecutorService 종료
     *
     * @param executor ExecutorService 인스턴스
     */
    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            // 30초 후에도 종료되지 않으면 강제 종료
            if (!executor.awaitTermination(terminateGraceTimeout, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(terminateForceTimeout, TimeUnit.SECONDS)) {
                    log.error("Executor did not terminate gracefully");
                }
            }
        } catch (InterruptedException e) {
            log.error("Executor shutdown interrupted", e);

            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 학과별 최신 공지 ID List를 Map으로 변환
     *
     * @param noticeRecentIdByDepartmentList [학과, 최신 공지 ID] 목록
     * @return { 학과: 최신 공지 ID } 구조 반환
     */
    private Map<Department, Long> transformNoticeRecentIdListToMap(
            List<NoticeRecentIdByDepartment> noticeRecentIdByDepartmentList) {
        return noticeRecentIdByDepartmentList.stream()
                .collect(Collectors.toMap(
                        NoticeRecentIdByDepartment::getDepartment,
                        NoticeRecentIdByDepartment::getRecentId
                ));
    }

    /**
     * 크롤링 결과 저장
     *
     * @param noticeDetailsSet 저장할 공지 세부 목록
     * @param noticeList       저장할 공지 목록
     * @param totalDepartments 저장할 학과 수
     */
    private void saveResults(Set<NoticeDetails> noticeDetailsSet, Set<Notice> noticeList, int totalDepartments) {
        try {
            if (noticeDetailsSet.isEmpty()) {
                log.info("No new notices found to save from {} departments", totalDepartments);
                return;
            }

            noticeDetailsRepository.saveAll(noticeDetailsSet);
            noticeRepository.saveAll(noticeList);
            log.info("Successfully saved {} notices from {} departments",
                    noticeList.size(), totalDepartments);
        } catch (Exception e) {
            log.error("Failed to save crawling results", e);
        }
    }
}
