package com.dongsoop.dongsoop.recruitment.study.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentApplyOverview;
import com.dongsoop.dongsoop.recruitment.dto.UpdateApplyStatusRequest;
import com.dongsoop.dongsoop.recruitment.entity.RecruitmentApplyStatus;
import com.dongsoop.dongsoop.recruitment.study.dto.ApplyStudyBoardRequest;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyApply;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyApply.StudyApplyKey;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoard;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoardDepartment;
import com.dongsoop.dongsoop.recruitment.study.exception.StudyBoardDepartmentMismatchException;
import com.dongsoop.dongsoop.recruitment.study.exception.StudyBoardDepartmentNotAssignedException;
import com.dongsoop.dongsoop.recruitment.study.exception.StudyBoardNotFound;
import com.dongsoop.dongsoop.recruitment.study.exception.StudyRecruitmentAlreadyAppliedException;
import com.dongsoop.dongsoop.recruitment.study.repository.StudyApplyRepository;
import com.dongsoop.dongsoop.recruitment.study.repository.StudyApplyRepositoryCustom;
import com.dongsoop.dongsoop.recruitment.study.repository.StudyBoardDepartmentRepository;
import com.dongsoop.dongsoop.recruitment.study.repository.StudyBoardRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudyApplyServiceImpl implements StudyApplyService {

    private final MemberService memberService;

    private final StudyApplyRepository studyApplyRepository;

    private final StudyBoardRepository studyBoardRepository;

    private final StudyBoardDepartmentRepository studyBoardDepartmentRepository;

    private final StudyApplyRepositoryCustom studyApplyRepositoryCustom;

    public void apply(ApplyStudyBoardRequest request) {
        Member member = memberService.getMemberReferenceByContext();
        validateAlreadyApplied(member.getId(), request.boardId());

        StudyBoard studyBoard = studyBoardRepository.findById(request.boardId())
                .orElseThrow(() -> new StudyBoardNotFound(request.boardId()));

        List<StudyBoardDepartment> studyBoardDepartmentList = studyBoardDepartmentRepository.findByStudyBoardId(
                request.boardId());
        if (studyBoardDepartmentList.isEmpty()) {
            throw new StudyBoardDepartmentNotAssignedException(request.boardId());
        }

        validateDepartment(studyBoardDepartmentList, member);

        StudyApplyKey key = new StudyApplyKey(studyBoard, member);
        StudyApply studyApplication = StudyApply.builder()
                .id(key)
                .introduction(request.introduction())
                .motivation(request.motivation())
                .build();

        studyApplyRepository.save(studyApplication);
    }

    private void validateAlreadyApplied(Long memberId, Long boardId) {
        boolean isAlreadyApplied = studyApplyRepositoryCustom.existsByBoardIdAndMemberId(boardId, memberId);
        if (isAlreadyApplied) {
            throw new StudyRecruitmentAlreadyAppliedException(memberId, boardId);
        }
    }

    private void validateDepartment(List<StudyBoardDepartment> boardDepartment, Member member) {
        DepartmentType requesterDepartmentType = member.getDepartment().getId();

        for (StudyBoardDepartment studyBoardDepartment : boardDepartment) {
            if (studyBoardDepartment.isSameDepartmentType(requesterDepartmentType)) {
                return;
            }
        }

        List<DepartmentType> boardDepartmentTypeList = boardDepartment.stream()
                .map(StudyBoardDepartment::getDepartment)
                .map(Department::getId)
                .toList();

        throw new StudyBoardDepartmentMismatchException(boardDepartmentTypeList, requesterDepartmentType);
    }

    @Override
    @Transactional
    public void updateStatus(Long boardId, UpdateApplyStatusRequest request) {
        if (request.status() == RecruitmentApplyStatus.APPLY) {
            return;
        }

        Long memberId = memberService.getMemberIdByAuthentication();

        studyApplyRepositoryCustom.updateApplyStatus(memberId, boardId, request.status());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecruitmentApplyOverview> getRecruitmentApplyOverview(Long boardId) {
        Long requesterId = memberService.getMemberIdByAuthentication();

        if (!studyBoardRepository.existsByBoardIdAndMemberId(boardId, requesterId)) {
            throw new StudyBoardNotFound(boardId, requesterId);
        }

        return studyApplyRepository.findApplyOverviewByBoardId(boardId, requesterId);
    }
}
