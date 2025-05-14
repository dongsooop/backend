package com.dongsoop.dongsoop.tutoring.service;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.tutoring.dto.CreateTutoringBoardRequest;
import com.dongsoop.dongsoop.tutoring.dto.TutoringBoardDetails;
import com.dongsoop.dongsoop.tutoring.dto.TutoringBoardOverview;
import com.dongsoop.dongsoop.tutoring.entity.TutoringBoard;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface TutoringBoardService {

    List<TutoringBoardOverview> getTutoringBoardByPage(DepartmentType departmentType, Pageable pageable);

    TutoringBoard create(CreateTutoringBoardRequest request);

    TutoringBoardDetails getTutoringBoardDetailsById(Long tutoringBoardId);
}
