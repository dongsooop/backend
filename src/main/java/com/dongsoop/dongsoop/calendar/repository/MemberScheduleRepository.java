package com.dongsoop.dongsoop.calendar.repository;

import com.dongsoop.dongsoop.calendar.entity.MemberSchedule;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberScheduleRepository extends JpaRepository<MemberSchedule, Long> {

    List<MemberSchedule> findByMember_IdAndStartAtIsGreaterThanEqualAndEndAtIsLessThan(Long memberId,
                                                                                       LocalDateTime startAt,
                                                                                       LocalDateTime endAt);
}
