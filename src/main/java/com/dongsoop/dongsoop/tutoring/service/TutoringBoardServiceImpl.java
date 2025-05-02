package com.dongsoop.dongsoop.tutoring.service;

import com.dongsoop.dongsoop.board.BoardDate;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.exception.domain.department.DepartmentNotFoundException;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.tutoring.dto.CreateTutoringBoardRequest;
import com.dongsoop.dongsoop.tutoring.dto.TutoringBoardOverview;
import com.dongsoop.dongsoop.tutoring.entity.TutoringBoard;
import com.dongsoop.dongsoop.tutoring.repository.TutoringBoardRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TutoringBoardServiceImpl implements TutoringBoardService {

    private final TutoringBoardRepository tutoringBoardRepository;

    private final DepartmentRepository departmentRepository;

    private final MemberService memberService;

    public List<TutoringBoardOverview> getAllTutoringBoard(DepartmentType departmentType) {
        Optional<Department> optionalRecruitmentDepartment = departmentRepository.findById(departmentType);
        Department recruitmentDepartment = optionalRecruitmentDepartment.orElseThrow(
                () -> new DepartmentNotFoundException(departmentType));

        return tutoringBoardRepository.findAllTutoringBoardOverviews(recruitmentDepartment);
    }

    public void create(CreateTutoringBoardRequest request) {
        TutoringBoard tutoringBoard = transformToTutoringBoard(request);
        tutoringBoardRepository.save(tutoringBoard);
    }

    private TutoringBoard transformToTutoringBoard(CreateTutoringBoardRequest request) {
        Member memberReference = memberService.getMemberReferenceByContext();

        DepartmentType departmentType = request.getDepartmentType();
        Department departmentReference = departmentRepository.getReferenceById(departmentType);

        return TutoringBoard.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .author(memberReference)
                .tags(request.getTags())
                .department(departmentReference)
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .recruitmentCapacity(request.getRecruitmentCapacity())
                .boardDate(new BoardDate())
                .build();
    }
}
