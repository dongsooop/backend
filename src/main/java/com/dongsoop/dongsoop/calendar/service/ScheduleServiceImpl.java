package com.dongsoop.dongsoop.calendar.service;

import com.dongsoop.dongsoop.calendar.dto.CreateMemberScheduleRequest;
import com.dongsoop.dongsoop.calendar.dto.MemberScheduleUpdateRequest;
import com.dongsoop.dongsoop.calendar.dto.ScheduleDetails;
import com.dongsoop.dongsoop.calendar.entity.MemberSchedule;
import com.dongsoop.dongsoop.calendar.entity.OfficialSchedule;
import com.dongsoop.dongsoop.calendar.repository.MemberScheduleRepository;
import com.dongsoop.dongsoop.calendar.repository.MemberScheduleRepositoryCustom;
import com.dongsoop.dongsoop.calendar.repository.OfficialScheduleRepositoryCustom;
import com.dongsoop.dongsoop.exception.domain.schedule.MemberScheduleNotFoundException;
import com.dongsoop.dongsoop.exception.domain.schedule.NotScheduleOwnerException;
import com.dongsoop.dongsoop.member.service.MemberService;
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

    private final MemberService memberService;

    public MemberSchedule createMemberSchedule(CreateMemberScheduleRequest createMemberScheduleRequest) {
        MemberSchedule schedule = createMemberScheduleRequest.toEntity();
        return memberScheduleRepository.save(schedule);
    }

    public List<ScheduleDetails> getMemberSchedule(Long memberId, YearMonth yearMonth) {
        LocalDate startMonth = LocalDate.of(yearMonth.getYear(), yearMonth.getMonth(), 1);
        YearMonth oneMonthLater = yearMonth.plusMonths(1);
        LocalDate endMonth = LocalDate.of(oneMonthLater.getYear(), oneMonthLater.getMonth(), 1);

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

    public void deleteMemberSchedule(Long scheduleId) {
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
