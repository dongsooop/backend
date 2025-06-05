package com.dongsoop.dongsoop.recruitment.tutoring.service;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.CreateTutoringBoardRequest;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.TutoringBoardDetails;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.TutoringBoardOverview;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.TutoringBoard;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface TutoringBoardService {

    List<TutoringBoardOverview> getBoardByPage(DepartmentType departmentType, Pageable pageable);

    TutoringBoard create(CreateTutoringBoardRequest request);

    TutoringBoardDetails getBoardDetailsById(Long boardId);
}
