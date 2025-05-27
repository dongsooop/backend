package com.dongsoop.dongsoop.project.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.exception.domain.project.ProjectBoardNotFound;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.project.dto.CreateProjectBoardRequest;
import com.dongsoop.dongsoop.project.dto.ProjectBoardDetails;
import com.dongsoop.dongsoop.project.dto.ProjectBoardOverview;
import com.dongsoop.dongsoop.project.entity.ProjectBoard;
import com.dongsoop.dongsoop.project.entity.ProjectBoardDepartment;
import com.dongsoop.dongsoop.project.entity.ProjectBoardDepartment.ProjectBoardDepartmentId;
import com.dongsoop.dongsoop.project.repository.ProjectBoardDepartmentRepository;
import com.dongsoop.dongsoop.project.repository.ProjectBoardRepository;
import com.dongsoop.dongsoop.project.repository.ProjectBoardRepositoryCustom;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectBoardServiceImpl implements ProjectBoardService {

    private final ProjectBoardRepository projectBoardRepository;

    private final ProjectBoardRepositoryCustom projectBoardRepositoryCustom;

    private final MemberService memberService;

    private final DepartmentRepository departmentRepository;

    private final ProjectBoardDepartmentRepository projectBoardDepartmentRepository;

    @Transactional
    public ProjectBoard create(CreateProjectBoardRequest request) {
        ProjectBoard projectBoardToSave = transformToProjectBoard(request);
        List<Department> departmentList = getDepartmentReferenceList(request.getDepartmentTypeList());

        ProjectBoard projectBoard = projectBoardRepository.save(projectBoardToSave);
        List<ProjectBoardDepartment> projectBoardDepartmentList = departmentList.stream()
                .map(department -> {
                    ProjectBoardDepartmentId projectBoardDepartmentId = new ProjectBoardDepartmentId(projectBoard,
                            department);
                    return new ProjectBoardDepartment(projectBoardDepartmentId);
                })
                .toList();

        projectBoardDepartmentRepository.saveAll(projectBoardDepartmentList);
        return projectBoard;
    }

    public List<ProjectBoardOverview> getProjectBoardByPage(DepartmentType departmentType, Pageable pageable) {
        return projectBoardRepositoryCustom.findProjectBoardOverviewsByPage(departmentType, pageable);
    }

    public ProjectBoardDetails getProjectBoardDetails(Long projectBoardId) {
        return projectBoardRepositoryCustom.findProjectBoardDetails(projectBoardId)
                .orElseThrow(() -> new ProjectBoardNotFound(projectBoardId));
    }

    private ProjectBoard transformToProjectBoard(CreateProjectBoardRequest request) {
        Member member = memberService.getMemberReferenceByContext();

        return ProjectBoard.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .tags(request.getTags())
                .author(member)
                .build();
    }

    private List<Department> getDepartmentReferenceList(List<DepartmentType> departmentTypeList) {
        return departmentTypeList.stream()
                .map(departmentRepository::getReferenceById)
                .toList();
    }
}
