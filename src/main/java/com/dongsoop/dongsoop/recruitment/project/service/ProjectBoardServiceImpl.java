package com.dongsoop.dongsoop.recruitment.project.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.exception.domain.project.ProjectBoardNotFound;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.project.dto.CreateProjectBoardRequest;
import com.dongsoop.dongsoop.recruitment.project.dto.ProjectBoardDetails;
import com.dongsoop.dongsoop.recruitment.project.dto.ProjectBoardOverview;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectBoard;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectBoardDepartment;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectBoardDepartment.ProjectBoardDepartmentId;
import com.dongsoop.dongsoop.recruitment.project.repository.ProjectBoardDepartmentRepository;
import com.dongsoop.dongsoop.recruitment.project.repository.ProjectBoardRepository;
import com.dongsoop.dongsoop.recruitment.project.repository.ProjectBoardRepositoryCustom;
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
        List<Department> departmentList = getDepartmentReferenceList(request.departmentTypeList());

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

    public List<ProjectBoardOverview> getBoardByPageAndDepartmentType(DepartmentType departmentType,
                                                                      Pageable pageable) {
        return projectBoardRepositoryCustom.findProjectBoardOverviewsByPageAndDepartmentType(departmentType, pageable);
    }

    public List<ProjectBoardOverview> getBoardByPage(Pageable pageable) {
        return projectBoardRepositoryCustom.findProjectBoardOverviewsByPage(pageable);
    }

    public ProjectBoardDetails getBoardDetailsById(Long projectBoardId) {
        return projectBoardRepositoryCustom.findProjectBoardDetails(projectBoardId)
                .orElseThrow(() -> new ProjectBoardNotFound(projectBoardId));
    }

    private ProjectBoard transformToProjectBoard(CreateProjectBoardRequest request) {
        Member member = memberService.getMemberReferenceByContext();

        return ProjectBoard.builder()
                .title(request.title())
                .content(request.content())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .tags(request.tags())
                .author(member)
                .build();
    }

    private List<Department> getDepartmentReferenceList(List<DepartmentType> departmentTypeList) {
        return departmentTypeList.stream()
                .map(departmentRepository::getReferenceById)
                .toList();
    }
}
