package com.dongsoop.dongsoop.home.service;

import com.dongsoop.dongsoop.calendar.dto.HomeSchedule;
import com.dongsoop.dongsoop.calendar.repository.MemberScheduleRepository;
import com.dongsoop.dongsoop.calendar.repository.OfficialScheduleRepository;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.home.dto.HomeDto;
import com.dongsoop.dongsoop.home.exception.HomeAsyncException;
import com.dongsoop.dongsoop.notice.dto.HomeNotice;
import com.dongsoop.dongsoop.notice.repository.NoticeRepository;
import com.dongsoop.dongsoop.recruitment.board.dto.HomeRecruitment;
import com.dongsoop.dongsoop.recruitment.repository.RecruitmentRepository;
import com.dongsoop.dongsoop.timetable.dto.HomeTimetable;
import com.dongsoop.dongsoop.timetable.entity.SemesterType;
import com.dongsoop.dongsoop.timetable.repository.TimetableRepository;
import java.time.LocalDate;
import java.time.Year;
import java.util.Comparator;
import java.util.List;
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

    @Value("${home.async.timeout.seconds:3}")
    private int TIMEOUT_SECONDS;

    public HomeDto getHome(Long requesterId, DepartmentType departmentType) {
        LocalDate today = LocalDate.now();
        Year year = Year.now();
        int month = today.getMonthValue();
        SemesterType semester = SemesterType.fromMonth(month);

        CompletableFuture<List<HomeTimetable>> fTimetable = call(
                () -> timetableRepository.searchHomeTimetable(requesterId, year, semester));
        CompletableFuture<List<HomeSchedule>> fMemberSchedules = call(
                () -> memberScheduleRepository.searchHomeSchedule(requesterId, today));
        CompletableFuture<List<HomeSchedule>> fOfficialSchedules = call(
                () -> officialScheduleRepository.searchHomeSchedule(today));
        CompletableFuture<List<HomeNotice>> fNotices = call(() -> noticeRepository.searchHomeNotices(departmentType));
        CompletableFuture<List<HomeRecruitment>> fRecruitments = call(recruitmentRepository::searchHomeRecruitment);

        // 모든 Future 완료 대기
        CompletableFuture.allOf(
                fTimetable, fMemberSchedules, fOfficialSchedules, fNotices, fRecruitments
        ).join();

        // 결과 조합
        List<HomeTimetable> timetable = fTimetable.join();
        List<HomeSchedule> schedules = Stream.of(fMemberSchedules, fOfficialSchedules)
                .flatMap((f) -> f.join().stream())
                .sorted(Comparator.comparing(HomeSchedule::startAt).thenComparing(HomeSchedule::endAt))
                .toList();
        List<HomeNotice> notices = fNotices.join();
        List<HomeRecruitment> popularRecruitments = fRecruitments.join();

        return new HomeDto(timetable, schedules, notices, popularRecruitments);
    }

    public <T> CompletableFuture<T> call(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, homeThreadExecutor)
                .orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .exceptionally(e -> {
                    log.error("thrown exception when collect home need's data", e);
                    throw new HomeAsyncException(e);
                });
    }
}
