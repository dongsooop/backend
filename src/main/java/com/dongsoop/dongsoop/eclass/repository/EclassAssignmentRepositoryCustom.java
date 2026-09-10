package com.dongsoop.dongsoop.eclass.repository;

import com.dongsoop.dongsoop.eclass.entity.EclassAssignment;
import java.time.LocalDateTime;
import java.util.List;

public interface EclassAssignmentRepositoryCustom {

    List<EclassAssignment> searchReminderTargets(LocalDateTime from, LocalDateTime to);

    List<EclassAssignment> searchUpcomingByDevice(Long deviceId, LocalDateTime now, int limit);

    List<EclassAssignment> searchUpcomingByMember(Long memberId, LocalDateTime now, int limit);

    long countUpcomingByDevice(Long deviceId, LocalDateTime now);

    long countUpcomingByMember(Long memberId, LocalDateTime now);
}
