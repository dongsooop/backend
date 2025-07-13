package com.dongsoop.dongsoop.recruitment.project.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentApplyOverview;
import com.dongsoop.dongsoop.recruitment.dto.UpdateApplyStatusRequest;
import com.dongsoop.dongsoop.recruitment.entity.RecruitmentApplyStatus;
import com.dongsoop.dongsoop.recruitment.project.dto.ApplyProjectBoardRequest;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectApply;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectApply.ProjectApplyKey;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectBoard;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectBoardDepartment;
import com.dongsoop.dongsoop.recruitment.project.exception.ProjectApplyNotFoundException;
import com.dongsoop.dongsoop.recruitment.project.exception.ProjectBoardDepartmentMismatchException;
import com.dongsoop.dongsoop.recruitment.project.exception.ProjectBoardDepartmentNotAssignedException;
import com.dongsoop.dongsoop.recruitment.project.exception.ProjectBoardNotFound;
import com.dongsoop.dongsoop.recruitment.project.exception.ProjectRecruitmentAlreadyAppliedException;
import com.dongsoop.dongsoop.recruitment.project.repository.ProjectApplyRepository;
import com.dongsoop.dongsoop.recruitment.project.repository.ProjectApplyRepositoryCustom;
import com.dongsoop.dongsoop.recruitment.project.repository.ProjectBoardDepartmentRepository;
import com.dongsoop.dongsoop.recruitment.project.repository.ProjectBoardRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectApplyServiceImpl implements ProjectApplyService {

    private final MemberService memberService;

    private final ProjectApplyRepository projectApplyRepository;

    private final ProjectBoardRepository projectBoardRepository;

    private final ProjectBoardDepartmentRepository projectBoardDepartmentRepository;

    private final ProjectApplyRepositoryCustom projectApplyRepositoryCustom;

    public void apply(ApplyProjectBoardRequest request) {
        Member member = memberService.getMemberReferenceByContext();
        validateAlreadyApplied(member.getId(), request.boardId());

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

    private void validateAlreadyApplied(Long memberId, Long boardId) {
        boolean isAlreadyApplied = projectApplyRepositoryCustom.existsByBoardIdAndMemberId(boardId, memberId);
        if (isAlreadyApplied) {
            throw new ProjectRecruitmentAlreadyAppliedException(memberId, boardId);
        }
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

    @Override
    @Transactional
    public void updateStatus(Long boardId, UpdateApplyStatusRequest request) {
        Long boardOwnerId = memberService.getMemberIdByAuthentication();
        if (!projectBoardRepository.existsByIdAndAuthorId(boardId, boardOwnerId)) {
            throw new ProjectBoardNotFound(boardId, boardOwnerId);
        }

        if (request.compareStatus(RecruitmentApplyStatus.APPLY)) {
            return;
        }

        projectApplyRepositoryCustom.updateApplyStatus(request.applierId(), boardId, request.status());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecruitmentApplyOverview> getRecruitmentApplyOverview(Long boardId) {
        Long requesterId = memberService.getMemberIdByAuthentication();

        if (!projectBoardRepository.existsByIdAndAuthorId(boardId, requesterId)) {
            throw new ProjectBoardNotFound(boardId, requesterId);
        }

        return projectApplyRepository.findApplyOverviewByBoardId(boardId, requesterId);
    }

    @Override
    public ApplyDetails getRecruitmentApplyDetails(Long boardId, Long applierId) {
        Long authorId = memberService.getMemberIdByAuthentication();

        // 게시물 주인이거나 지원자가 아닐 경우 확인할 수 없다.
        if (!projectBoardRepository.existsByIdAndAuthorId(boardId, authorId)
                && !projectApplyRepositoryCustom.existsByBoardIdAndMemberId(boardId, applierId)) {
            throw new ProjectBoardNotFound(boardId);
        }

        return projectApplyRepositoryCustom.findApplyDetailsByBoardIdAndApplierId(boardId, applierId)
                .orElseThrow(() -> new ProjectApplyNotFoundException(boardId, applierId));
    }
}
