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

    @Override
    public MemberSchedule createMemberSchedule(Member requester,
                                               CreateMemberScheduleRequest createMemberScheduleRequest) {
        MemberSchedule schedule = createMemberScheduleRequest.toEntity();
        schedule.setMember(requester);

        return memberScheduleRepository.save(schedule);
    }

    @Override
    public List<ScheduleDetails> getSchedule(Long memberId, YearMonth yearMonth) {
        LocalDate startDate = getStartDate(yearMonth);
        LocalDate endDate = getEndDate(yearMonth);

        List<ScheduleDetails> officialScheduleDetails = getOfficialSchedule(startDate, endDate);
        List<ScheduleDetails> memberScheduleDetails = getMemberSchedule(memberId, startDate, endDate);

        List<ScheduleDetails> totalScheduleDetails = new ArrayList<>();
        totalScheduleDetails.addAll(officialScheduleDetails);
        totalScheduleDetails.addAll(memberScheduleDetails);

        totalScheduleDetails.sort(Comparator.comparing(ScheduleDetails::getStartAt));

        return totalScheduleDetails;
    }

    @Override
    public List<ScheduleDetails> getSchedule(YearMonth yearMonth) {
        LocalDate startDate = getStartDate(yearMonth);
        LocalDate endDate = getEndDate(yearMonth);

        List<ScheduleDetails> officialScheduleDetails = getOfficialSchedule(startDate, endDate);

        return officialScheduleDetails.stream()
                .sorted((a, b) -> a.getStartAt().compareTo(b.getStartAt()))
                .toList();
    }

    private List<ScheduleDetails> getOfficialSchedule(LocalDate startDate, LocalDate endDate) {
        List<OfficialSchedule> officialScheduleList = officialScheduleRepository.findOfficialScheduleByDuration(
                startDate, endDate);

        return officialScheduleList.stream()
                .map(OfficialSchedule::toDetails)
                .toList();
    }

    private List<ScheduleDetails> getMemberSchedule(Long memberId, LocalDate startDate, LocalDate endDate) {
        List<MemberSchedule> memberScheduleList = memberScheduleRepository.findMemberScheduleByDuration(
                memberId, startDate.atStartOfDay(), endDate.atStartOfDay());

        return memberScheduleList.stream()
                .map(MemberSchedule::toDetails)
                .toList();
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
        // 이번달 마지막 날
        LocalDate firstDayOfNextMonth = yearMonth.atEndOfMonth();

        long week = firstDayOfNextMonth.getDayOfWeek().getValue();

        // 그 주의 시작일
        return firstDayOfNextMonth.plusDays(7L - (week % 7));
    }

    @Override
    public void deleteMemberSchedule(Long scheduleId, Long requesterId) {
        MemberSchedule schedule = memberScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new MemberScheduleNotFoundException(scheduleId));

        Long scheduleOwnerId = schedule.getMember()
                .getId();

        validateScheduleOwner(scheduleOwnerId, scheduleId, requesterId);

        memberScheduleRepository.deleteById(scheduleId);
    }

    @Override
    public void updateMemberSchedule(Long scheduleId, Long requesterId, MemberScheduleUpdateRequest request) {
        MemberSchedule schedule = memberScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new MemberScheduleNotFoundException(scheduleId));

        Long scheduleOwnerId = schedule.getMember()
                .getId();

        validateScheduleOwner(scheduleOwnerId, scheduleId, requesterId);
        schedule.update(request);

        memberScheduleRepository.save(schedule);
    }

    /**
     * 요청을 보낸 회원과 일정의 주인이 같은지 검증한다.
     */
    private void validateScheduleOwner(Long scheduleOwnerId, Long scheduleId, Long requesterId) {
        if (!scheduleOwnerId.equals(requesterId)) {
            throw new NotScheduleOwnerException(requesterId, scheduleOwnerId, scheduleId);
        }
    }
}
