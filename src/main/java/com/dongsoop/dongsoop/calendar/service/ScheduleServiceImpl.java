package com.dongsoop.dongsoop.calendar.service;

import com.dongsoop.dongsoop.calendar.dto.CreateMemberScheduleRequest;
import com.dongsoop.dongsoop.calendar.dto.MemberScheduleUpdateRequest;
import com.dongsoop.dongsoop.calendar.dto.ScheduleDetails;
import com.dongsoop.dongsoop.calendar.entity.MemberSchedule;
import com.dongsoop.dongsoop.calendar.entity.OfficialSchedule;
import com.dongsoop.dongsoop.calendar.exception.MemberScheduleNotFoundException;
import com.dongsoop.dongsoop.calendar.exception.NotScheduleOwnerException;
import com.dongsoop.dongsoop.calendar.repository.MemberScheduleRepository;
import com.dongsoop.dongsoop.calendar.repository.OfficialScheduleRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final MemberScheduleRepository memberScheduleRepository;

    private final OfficialScheduleRepository officialScheduleRepository;

    private final MemberService memberService;

    public MemberSchedule createMemberSchedule(CreateMemberScheduleRequest createMemberScheduleRequest) {
        MemberSchedule schedule = createMemberScheduleRequest.toEntity();
        Member member = memberService.getMemberReferenceByContext();
        schedule.setMember(member);

        return memberScheduleRepository.save(schedule);
    }

    public List<ScheduleDetails> getMemberSchedule(Long memberId, YearMonth yearMonth) {
        LocalDate startDate = getStartDate(yearMonth);
        LocalDate endDate = getEndDate(yearMonth);

        List<OfficialSchedule> officialScheduleList = officialScheduleRepository.findOfficialScheduleByDuration(
                startDate, endDate);

        List<MemberSchedule> memberScheduleList = memberScheduleRepository.findMemberScheduleByDuration(
                memberId, startDate.atStartOfDay(), endDate.atStartOfDay());

        List<ScheduleDetails> officialScheduleDetails = officialScheduleList.stream()
                .map(OfficialSchedule::toDetails)
                .toList();

        List<ScheduleDetails> memberScheduleDetails = memberScheduleList.stream()
                .map(MemberSchedule::toDetails)
                .toList();

        List<ScheduleDetails> totalScheduleDetails = new ArrayList<>();
        totalScheduleDetails.addAll(officialScheduleDetails);
        totalScheduleDetails.addAll(memberScheduleDetails);

        totalScheduleDetails.sort(Comparator.comparing(ScheduleDetails::getStartAt));

        return totalScheduleDetails;
    }

    private LocalDate getStartDate(YearMonth yearMonth) {
        // 지난달 마지막 날
        LocalDate lastDayOfLastMonth = yearMonth.minusMonths(1L)
                .atEndOfMonth();

        int week = lastDayOfLastMonth.getDayOfWeek().getValue();

        // 그 주의 시작일
        return lastDayOfLastMonth.minusDays(week % 7);
    }

    private LocalDate getEndDate(YearMonth yearMonth) {
        // 다음달 마지막 첫째 날
        LocalDate firstDayOfNextMonth = yearMonth.plusMonths(1L)
                .atEndOfMonth();

        long week = firstDayOfNextMonth.getDayOfWeek().getValue();

        // 그 주의 시작일
        return firstDayOfNextMonth.plusDays(7L - week);
    }

    public void deleteMemberSchedule(Long scheduleId) {
        MemberSchedule schedule = memberScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new MemberScheduleNotFoundException(scheduleId));

        Long scheduleOwnerId = schedule.getMember()
                .getId();

        validateScheduleOwner(scheduleOwnerId, scheduleId);

        memberScheduleRepository.deleteById(scheduleId);
    }

    public void updateMemberSchedule(Long scheduleId, MemberScheduleUpdateRequest request) {
        MemberSchedule schedule = memberScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new MemberScheduleNotFoundException(scheduleId));

        Long scheduleOwnerId = schedule.getMember()
                .getId();

        validateScheduleOwner(scheduleOwnerId, scheduleId);
        schedule.update(request);

        memberScheduleRepository.save(schedule);
    }

    /**
     * 요청을 보낸 회원과 일정의 주인이 같은지 검증한다.
     */
    private void validateScheduleOwner(Long scheduleOwnerId, Long scheduleId) {
        Long requestMemberId = memberService.getMemberReferenceByContext()
                .getId();

        if (!scheduleOwnerId.equals(requestMemberId)) {
            throw new NotScheduleOwnerException(requestMemberId, scheduleOwnerId, scheduleId);
        }
    }
}
