package com.dongsoop.dongsoop.eclass.repository;

import com.dongsoop.dongsoop.eclass.entity.EclassAssignment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EclassAssignmentRepository extends JpaRepository<EclassAssignment, Long>,
        EclassAssignmentRepositoryCustom {

    List<EclassAssignment> findAllByLinkId(Long linkId);

    void deleteAllByLinkId(Long linkId);
}
