package com.dongsoop.dongsoop.eclass.repository;

import com.dongsoop.dongsoop.eclass.entity.EclassAssignment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EclassAssignmentRepository extends JpaRepository<EclassAssignment, Long> {

    List<EclassAssignment> findByMemberIdAndIsDeletedFalseOrderByCreatedAtDesc(Long memberId);

    Optional<EclassAssignment> findByMemberIdAndEclassIdAndIsDeletedFalse(Long memberId, String eclassId);
}
