package com.dongsoop.dongsoop.recruitment.tutoring.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.exception.domain.department.DepartmentNotFoundException;
import com.dongsoop.dongsoop.exception.domain.tutoring.TutoringBoardNotFound;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.CreateTutoringBoardRequest;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.TutoringBoardDetails;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.TutoringBoardOverview;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.TutoringBoard;
import com.dongsoop.dongsoop.recruitment.tutoring.repository.TutoringBoardRepository;
import com.dongsoop.dongsoop.recruitment.tutoring.repository.TutoringBoardRepositoryCustom;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TutoringBoardServiceImpl implements TutoringBoardService {

    private final TutoringBoardRepository tutoringBoardRepository;

    private final TutoringBoardRepositoryCustom tutoringBoardRepositoryCustom;

    private final DepartmentRepository departmentRepository;

    private final MemberService memberService;

    public List<TutoringBoardOverview> getTutoringBoardByPage(DepartmentType departmentType, Pageable pageable) {
        Optional<Department> optionalRecruitmentDepartment = departmentRepository.findById(departmentType);
        Department recruitmentDepartment = optionalRecruitmentDepartment.orElseThrow(
                () -> new DepartmentNotFoundException(departmentType));

        return tutoringBoardRepositoryCustom.findTutoringBoardOverviewsByPage(recruitmentDepartment, pageable);
    }

    public TutoringBoard create(CreateTutoringBoardRequest request) {
        TutoringBoard tutoringBoard = transformToTutoringBoard(request);
        return tutoringBoardRepository.save(tutoringBoard);
    }

    public TutoringBoardDetails getTutoringBoardDetailsById(Long tutoringBoardId) {
        return tutoringBoardRepositoryCustom.findInformationById(tutoringBoardId)
                .orElseThrow(() -> new TutoringBoardNotFound(tutoringBoardId));
    }

    private TutoringBoard transformToTutoringBoard(CreateTutoringBoardRequest request) {
        Member memberReference = memberService.getMemberReferenceByContext();

        List<DepartmentType> departmentTypeList = request.getDepartmentTypeList();
        Department departmentReference = departmentRepository.getReferenceById(departmentTypeList.get(0));

        return TutoringBoard.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .author(memberReference)
                .tags(request.getTags())
                .department(departmentReference)
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .build();
    }
}
