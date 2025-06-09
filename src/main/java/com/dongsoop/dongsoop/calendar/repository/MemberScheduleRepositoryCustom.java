package com.dongsoop.dongsoop.calendar.repository;

import com.dongsoop.dongsoop.calendar.entity.MemberSchedule;
import java.time.LocalDateTime;
import java.util.List;

public interface MemberScheduleRepositoryCustom {

    List<MemberSchedule> findMemberScheduleByDuration(Long memberId, LocalDateTime startAt, LocalDateTime endAt);
}
