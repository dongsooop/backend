package com.dongsoop.dongsoop.home.service;

import com.dongsoop.dongsoop.calendar.dto.HomeSchedule;
import com.dongsoop.dongsoop.calendar.repository.MemberScheduleRepository;
import com.dongsoop.dongsoop.calendar.repository.OfficialScheduleRepository;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.home.dto.HomeDto;
import com.dongsoop.dongsoop.notice.dto.HomeNotice;
import com.dongsoop.dongsoop.notice.repository.NoticeRepository;
import com.dongsoop.dongsoop.recruitment.board.dto.HomeRecruitment;
import com.dongsoop.dongsoop.recruitment.repository.RecruitmentRepository;
import com.dongsoop.dongsoop.timetable.dto.HomeTimetable;
import com.dongsoop.dongsoop.timetable.entity.SemesterType;
import com.dongsoop.dongsoop.timetable.repository.TimetableRepository;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final TimetableRepository timetableRepository;
    private final OfficialScheduleRepository officialScheduleRepository;
    private final MemberScheduleRepository memberScheduleRepository;
    private final NoticeRepository noticeRepository;
    private final RecruitmentRepository recruitmentRepository;

    public HomeDto getHome(Long requesterId, DepartmentType departmentType) {
        LocalDate today = LocalDate.now();
        Year year = Year.now();
        int month = LocalDate.now().getMonthValue();

        List<HomeTimetable> timetable = timetableRepository.searchHomeTimetable(requesterId, year,
                SemesterType.fromMonth(month));

        List<HomeSchedule> memberSchedules = memberScheduleRepository.searchHomeSchedule(requesterId, today);
        List<HomeSchedule> officialSchedules = officialScheduleRepository.searchHomeSchedule(today);
        List<HomeSchedule> schedules = new ArrayList<>();
        schedules.addAll(memberSchedules);
        schedules.addAll(officialSchedules);

        schedules.sort((s1, s2) -> {
            int compareStartAt = s1.startAt().compareTo(s2.startAt());
            if (compareStartAt != 0) {
                return compareStartAt;
            }

            return s1.endAt().compareTo(s2.endAt());
        });

        List<HomeNotice> notices = noticeRepository.searchHomeNotices(departmentType);
        List<HomeRecruitment> popular_recruitments = recruitmentRepository.searchHomeRecruitment();

        return new HomeDto(timetable, schedules, notices, popular_recruitments);
    }
}
