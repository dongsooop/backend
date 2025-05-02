package com.dongsoop.dongsoop.tutoring.service;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.tutoring.dto.CreateTutoringBoardRequest;
import com.dongsoop.dongsoop.tutoring.dto.TutoringBoardOverview;
import java.util.List;

public interface TutoringBoardService {

    List<TutoringBoardOverview> getAllTutoringBoard(DepartmentType departmentType);

    void create(CreateTutoringBoardRequest request);
}
