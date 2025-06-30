package com.dongsoop.dongsoop.recruitment.tutoring.service;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.CreateTutoringBoardRequest;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.TutoringBoard;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface TutoringBoardService {

    List<RecruitmentOverview> getBoardByPageAndDepartmentType(DepartmentType departmentType, Pageable pageable);

    List<RecruitmentOverview> getBoardByPage(Pageable pageable);

    TutoringBoard create(CreateTutoringBoardRequest request);

    RecruitmentDetails getBoardDetailsById(Long boardId);
}
