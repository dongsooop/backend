package com.dongsoop.dongsoop.tutoring.repository;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.tutoring.dto.TutoringBoardDetails;
import com.dongsoop.dongsoop.tutoring.dto.TutoringBoardOverview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface TutoringBoardRepositoryCustom {

    List<TutoringBoardOverview> findTutoringBoardOverviewsByPage(Department recruitmentDepartment,
                                                                 Pageable pageable);

    Optional<TutoringBoardDetails> findInformationById(Long tutoringBoardId);
}
