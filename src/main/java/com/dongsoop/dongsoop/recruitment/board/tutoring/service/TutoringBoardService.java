package com.dongsoop.dongsoop.recruitment.board.tutoring.service;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.board.tutoring.dto.CreateTutoringBoardRequest;
import com.dongsoop.dongsoop.recruitment.board.tutoring.entity.TutoringBoard;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface TutoringBoardService {

    List<RecruitmentOverview> getBoardByPageAndDepartmentType(DepartmentType departmentType, Pageable pageable);

    List<RecruitmentOverview> getBoardByPage(Pageable pageable);

    TutoringBoard create(CreateTutoringBoardRequest request);

    RecruitmentDetails getBoardDetailsById(Long boardId);
}
