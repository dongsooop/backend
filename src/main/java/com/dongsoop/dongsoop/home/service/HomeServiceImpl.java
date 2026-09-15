package com.dongsoop.dongsoop.home.service;

import com.dongsoop.dongsoop.calendar.dto.HomeSchedule;
import com.dongsoop.dongsoop.calendar.repository.MemberScheduleRepository;
import com.dongsoop.dongsoop.calendar.repository.OfficialScheduleRepository;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.eclass.service.EclassAssignmentService;
import com.dongsoop.dongsoop.home.dto.HomeDto;
import com.dongsoop.dongsoop.home.dto.HomeEclassSummary;
import com.dongsoop.dongsoop.home.exception.HomeAsyncException;
import com.dongsoop.dongsoop.notice.dto.HomeNotice;
import com.dongsoop.dongsoop.notice.repository.NoticeRepository;
import com.dongsoop.dongsoop.recruitment.board.dto.HomeRecruitment;
import com.dongsoop.dongsoop.recruitment.repository.RecruitmentRepository;
import com.dongsoop.dongsoop.timetable.dto.HomeTimetable;
import com.dongsoop.dongsoop.timetable.entity.SemesterType;
import com.dongsoop.dongsoop.timetable.repository.TimetableRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    @Qualifier("homeThreadExecutor")
    private final ExecutorService homeThreadExecutor;

    private final TimetableRepository timetableRepository;
    private final OfficialScheduleRepository officialScheduleRepository;
    private final MemberScheduleRepository memberScheduleRepository;
    private final NoticeRepository noticeRepository;
    private final RecruitmentRepository recruitmentRepository;
    private final EclassAssignmentService eclassAssignmentService;

    @Value("${home.async.timeout.seconds:3}")
    private int TIMEOUT_SECONDS;

    @Override
    public HomeDto getHome(Long requesterId, DepartmentType departmentType, String fid, String deviceToken) {
        LocalDate today = LocalDate.now();
        Year year = Year.now();
        int month = today.getMonthValue();
        SemesterType semester = SemesterType.fromMonth(month);
        DayOfWeek week = today.getDayOfWeek();

        CompletableFuture<List<HomeTimetable>> fTimetable = call(
                () -> timetableRepository.searchHomeTimetable(requesterId, year, semester, week));
        CompletableFuture<List<HomeSchedule>> fMemberSchedules = call(
                () -> memberScheduleRepository.searchHomeSchedule(requesterId, today));
        CompletableFuture<List<HomeSchedule>> fOfficialSchedules = call(
                () -> officialScheduleRepository.searchHomeSchedule(today));
        CompletableFuture<List<HomeNotice>> fNotices = call(() -> noticeRepository.searchHomeNotices(departmentType));
        CompletableFuture<List<HomeRecruitment>> fRecruitments = call(
                () -> recruitmentRepository.searchHomeRecruitment(departmentType.name()));
        CompletableFuture<HomeEclassSummary> fEclass = call(
                () -> eclassAssignmentService.getHomeSummary(requesterId, fid, deviceToken));

        // 모든 Future 완료 대기
        CompletableFuture.allOf(
                fTimetable, fMemberSchedules, fOfficialSchedules, fNotices, fRecruitments, fEclass
        ).join();

        // 결과 조합
        List<HomeTimetable> timetable = fTimetable.join();
        List<HomeSchedule> schedules = Stream.of(fMemberSchedules, fOfficialSchedules)
                .flatMap((f) -> f.join().stream())
                .sorted(Comparator.comparing(HomeSchedule::startAt).thenComparing(HomeSchedule::endAt))
                .toList();
        List<HomeNotice> notices = fNotices.join();
        List<HomeRecruitment> popularRecruitments = fRecruitments.join();
        HomeEclassSummary eclassSummary = fEclass.join();

        return new HomeDto(timetable, schedules, notices, popularRecruitments, eclassSummary);
    }

    /**
     * 구독한 학과들 기준으로 개인화된 홈을 조합한다 (회원/비회원 공통).
     *
     * <p>공지는 {@code departmentTypes}가 비어 있어도 {@code noticeRepository.searchHomeNotices}가
     * 내부적으로 DEPT_1001(대학 공지)을 항상 포함시키므로 별도 분기가 필요 없다.
     *
     * <p>추천 공고는 학과 하나만 받는 기존 구조라 구독 학과 중 첫 번째만 반영한다
     * (다학과 미지원 — 필요하면 별도 확장). 구독이 0개면 학과 필터 없는 무인자 쿼리로 폴백한다.
     */
    @Override
    public HomeDto getHome(Set<DepartmentType> departmentTypes, String fid, String deviceToken) {
        LocalDate today = LocalDate.now();

        CompletableFuture<List<HomeSchedule>> fOfficialSchedules = call(
                () -> officialScheduleRepository.searchHomeSchedule(today));
        CompletableFuture<List<HomeNotice>> fNotices = call(() -> noticeRepository.searchHomeNotices(departmentTypes));
        CompletableFuture<List<HomeRecruitment>> fRecruitments = departmentTypes.isEmpty()
                ? call(recruitmentRepository::searchHomeRecruitment)
                : call(() -> recruitmentRepository.searchHomeRecruitment(
                        departmentTypes.iterator().next().name()));
        CompletableFuture<HomeEclassSummary> fEclass = call(
                () -> eclassAssignmentService.getHomeSummary(fid, deviceToken));

        // 모든 Future 완료 대기
        CompletableFuture.allOf(
                fOfficialSchedules, fNotices, fRecruitments, fEclass
        ).join();

        // 결과 조합
        List<HomeSchedule> schedules = fOfficialSchedules.join().stream()
                .sorted(Comparator.comparing(HomeSchedule::startAt).thenComparing(HomeSchedule::endAt))
                .toList();
        List<HomeNotice> notices = fNotices.join();
        List<HomeRecruitment> popularRecruitments = fRecruitments.join();
        HomeEclassSummary eclassSummary = fEclass.join();

        return new HomeDto(Collections.emptyList(), schedules, notices, popularRecruitments, eclassSummary);
    }

    private <T> CompletableFuture<T> call(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, homeThreadExecutor)
                .orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .exceptionally(e -> {
                    log.error("thrown exception when collect home need's data", e);
                    throw new HomeAsyncException(e);
                });
    }
}
