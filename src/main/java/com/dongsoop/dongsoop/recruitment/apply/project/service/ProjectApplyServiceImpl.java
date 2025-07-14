package com.dongsoop.dongsoop.recruitment.apply.project.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.apply.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.apply.dto.RecruitmentApplyOverview;
import com.dongsoop.dongsoop.recruitment.apply.dto.UpdateApplyStatusRequest;
import com.dongsoop.dongsoop.recruitment.apply.entity.RecruitmentApplyStatus;
import com.dongsoop.dongsoop.recruitment.apply.project.dto.ApplyProjectBoardRequest;
import com.dongsoop.dongsoop.recruitment.apply.project.entity.ProjectApply;
import com.dongsoop.dongsoop.recruitment.apply.project.entity.ProjectApply.ProjectApplyKey;
import com.dongsoop.dongsoop.recruitment.apply.project.exception.ProjectApplyNotFoundException;
import com.dongsoop.dongsoop.recruitment.apply.project.exception.ProjectRecruitmentAlreadyAppliedException;
import com.dongsoop.dongsoop.recruitment.apply.project.repository.ProjectApplyRepository;
import com.dongsoop.dongsoop.recruitment.apply.project.repository.ProjectApplyRepositoryCustom;
import com.dongsoop.dongsoop.recruitment.board.project.entity.ProjectBoard;
import com.dongsoop.dongsoop.recruitment.board.project.entity.ProjectBoardDepartment;
import com.dongsoop.dongsoop.recruitment.board.project.exception.ProjectBoardDepartmentMismatchException;
import com.dongsoop.dongsoop.recruitment.board.project.exception.ProjectBoardDepartmentNotAssignedException;
import com.dongsoop.dongsoop.recruitment.board.project.exception.ProjectBoardNotFound;
import com.dongsoop.dongsoop.recruitment.board.project.repository.ProjectBoardDepartmentRepository;
import com.dongsoop.dongsoop.recruitment.board.project.repository.ProjectBoardRepository;
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

        // 게시물 주인이 아닌 경우 예외
        if (!projectBoardRepository.existsByIdAndAuthorId(boardId, authorId)) {
            throw new ProjectBoardNotFound(boardId);
        }

        return projectApplyRepositoryCustom.findApplyDetailsByBoardIdAndApplierId(boardId, applierId)
                .orElseThrow(() -> new ProjectApplyNotFoundException(boardId, applierId));
    }

    @Override
    public ApplyDetails getRecruitmentApplyDetails(Long boardId) {
        Long requester = memberService.getMemberIdByAuthentication();

        return projectApplyRepositoryCustom.findApplyDetailsByBoardIdAndApplierId(boardId, requester)
                .orElseThrow(() -> new ProjectApplyNotFoundException(boardId, requester));
    }
}
