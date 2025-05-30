package com.dongsoop.dongsoop.calendar.service;

import com.dongsoop.dongsoop.calendar.dto.CreateMemberScheduleRequest;
import com.dongsoop.dongsoop.calendar.dto.ScheduleDetails;
import com.dongsoop.dongsoop.calendar.entity.MemberSchedule;
import com.dongsoop.dongsoop.calendar.entity.OfficialSchedule;
import com.dongsoop.dongsoop.calendar.repository.MemberScheduleRepository;
import com.dongsoop.dongsoop.calendar.repository.MemberScheduleRepositoryCustom;
import com.dongsoop.dongsoop.calendar.repository.OfficialScheduleRepositoryCustom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final MemberScheduleRepository memberScheduleRepository;

    private final MemberScheduleRepositoryCustom memberScheduleRepositoryCustom;

    private final OfficialScheduleRepositoryCustom commonScheduleRepositoryCustom;

    public MemberSchedule createMemberSchedule(CreateMemberScheduleRequest createMemberScheduleRequest) {
        MemberSchedule schedule = createMemberScheduleRequest.toEntity();
        return memberScheduleRepository.save(schedule);
    }

    public List<ScheduleDetails> getMemberSchedule(Long memberId, YearMonth yearMonth) {
        LocalDate startMonth = LocalDate.of(yearMonth.getYear(), yearMonth.getMonth(), 1);
        LocalDate endMonth = LocalDate.of(yearMonth.getYear(), yearMonth.getMonth().plus(1), 1);

        LocalDateTime startAt = startMonth.atStartOfDay();
        LocalDateTime endAt = endMonth.atStartOfDay();

        List<OfficialSchedule> officialSchedule = commonScheduleRepositoryCustom.findOfficialScheduleByDuration(
                startMonth, endMonth);

        List<MemberSchedule> memberSchedule = memberScheduleRepositoryCustom.findMemberScheduleByDuration(
                memberId, startAt, endAt);

        List<ScheduleDetails> officialScheduleDetails = officialSchedule.stream()
                .map(OfficialSchedule::toDetails)
                .toList();

        List<ScheduleDetails> memberScheduleDetails = memberSchedule.stream()
                .map(MemberSchedule::toDetails)
                .toList();

        List<ScheduleDetails> totalScheduleDetails = new ArrayList<>();
        totalScheduleDetails.addAll(officialScheduleDetails);
        totalScheduleDetails.addAll(memberScheduleDetails);

        return totalScheduleDetails;
    }
}
