package com.dongsoop.dongsoop.recruitment.project.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.exception.domain.project.ProjectBoardDepartmentMismatchException;
import com.dongsoop.dongsoop.exception.domain.project.ProjectBoardDepartmentNotAssignedException;
import com.dongsoop.dongsoop.exception.domain.project.ProjectBoardNotFound;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.project.dto.ApplyProjectBoardRequest;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectApply;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectApply.ProjectApplyKey;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectBoard;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectBoardDepartment;
import com.dongsoop.dongsoop.recruitment.project.repository.ProjectApplyRepository;
import com.dongsoop.dongsoop.recruitment.project.repository.ProjectBoardDepartmentRepository;
import com.dongsoop.dongsoop.recruitment.project.repository.ProjectBoardRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectApplyServiceImpl implements ProjectApplyService {

    private final MemberService memberService;

    private final ProjectApplyRepository projectApplyRepository;

    private final ProjectBoardRepository projectBoardRepository;

    private final ProjectBoardDepartmentRepository projectBoardDepartmentRepository;

    public void apply(ApplyProjectBoardRequest request) {
        Member member = memberService.getMemberReferenceByContext();
        ProjectBoard projectBoard = projectBoardRepository.findById(request.boardId())
                .orElseThrow(() -> new ProjectBoardNotFound(request.boardId()));

        List<ProjectBoardDepartment> boardDepartmentList = projectBoardDepartmentRepository.findByProjectBoardId(
                request.boardId());
        if (boardDepartmentList.isEmpty()) {
            throw new ProjectBoardDepartmentNotAssignedException(request.boardId());
        }

        validateDepartment(boardDepartmentList, member);

        ProjectApplyKey key = new ProjectApplyKey(projectBoard, member);
        ProjectApply boardApply = ProjectApply.builder()
                .id(key)
                .introduction(request.introduction())
                .motivation(request.motivation())
                .build();

        projectApplyRepository.save(boardApply);
    }

    private void validateDepartment(List<ProjectBoardDepartment> boardDepartmentList, Member member) {
        DepartmentType requesterDepartmentType = member.getDepartment().getId();

        for (ProjectBoardDepartment boardDepartment : boardDepartmentList) {
            if (boardDepartment.isSameDepartmentType(requesterDepartmentType)) {
                return;
            }
        }

        List<DepartmentType> boardDepartmentTypeList = boardDepartmentList.stream()
                .map(ProjectBoardDepartment::getDepartment)
                .map(Department::getId)
                .toList();

        throw new ProjectBoardDepartmentMismatchException(boardDepartmentTypeList, requesterDepartmentType);
    }
}
