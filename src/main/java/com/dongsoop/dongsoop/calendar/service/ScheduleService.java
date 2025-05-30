package com.dongsoop.dongsoop.calendar.service;

import com.dongsoop.dongsoop.calendar.dto.CreateMemberScheduleRequest;
import com.dongsoop.dongsoop.calendar.dto.ScheduleDetails;
import com.dongsoop.dongsoop.calendar.entity.MemberSchedule;
import java.time.YearMonth;
import java.util.List;

public interface ScheduleService {

    MemberSchedule createMemberSchedule(CreateMemberScheduleRequest createMemberScheduleRequest);

    List<ScheduleDetails> getMemberSchedule(Long memberId, YearMonth yearMonth);
}
