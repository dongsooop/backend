package com.dongsoop.dongsoop.tutoring.service;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.tutoring.dto.CreateTutoringBoardRequest;
import com.dongsoop.dongsoop.tutoring.dto.TutoringBoardOverview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TutoringBoardService {

    Page<TutoringBoardOverview> getTutoringBoardByPage(DepartmentType departmentType, Pageable pageable);

    void create(CreateTutoringBoardRequest request);
}
