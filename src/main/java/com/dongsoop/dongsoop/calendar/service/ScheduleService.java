package com.dongsoop.dongsoop.calendar.service;

import com.dongsoop.dongsoop.calendar.dto.CreateMemberScheduleRequest;
import com.dongsoop.dongsoop.calendar.dto.MemberScheduleUpdateRequest;
import com.dongsoop.dongsoop.calendar.dto.ScheduleDetails;
import com.dongsoop.dongsoop.calendar.entity.MemberSchedule;
import com.dongsoop.dongsoop.member.entity.Member;
import java.time.YearMonth;
import java.util.List;

public interface ScheduleService {

    MemberSchedule createMemberSchedule(Member requester, CreateMemberScheduleRequest createMemberScheduleRequest);

    List<ScheduleDetails> getSchedule(Long memberId, YearMonth yearMonth);

    List<ScheduleDetails> getSchedule(YearMonth yearMonth);

    void deleteMemberSchedule(Long scheduleId, Long requesterId);

    void updateMemberSchedule(Long scheduleId, Long requesterId, MemberScheduleUpdateRequest request);
}
