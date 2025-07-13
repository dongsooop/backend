package com.dongsoop.dongsoop.recruitment.tutoring.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentApplyOverview;
import com.dongsoop.dongsoop.recruitment.dto.UpdateApplyStatusRequest;
import com.dongsoop.dongsoop.recruitment.entity.RecruitmentApplyStatus;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.ApplyTutoringBoardRequest;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.TutoringApply;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.TutoringApply.TutoringApplyKey;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.TutoringBoard;
import com.dongsoop.dongsoop.recruitment.tutoring.exception.TutoringApplyNotFoundException;
import com.dongsoop.dongsoop.recruitment.tutoring.exception.TutoringBoardDepartmentMismatchException;
import com.dongsoop.dongsoop.recruitment.tutoring.exception.TutoringBoardNotFound;
import com.dongsoop.dongsoop.recruitment.tutoring.exception.TutoringRecruitmentAlreadyAppliedException;
import com.dongsoop.dongsoop.recruitment.tutoring.repository.TutoringApplyRepository;
import com.dongsoop.dongsoop.recruitment.tutoring.repository.TutoringApplyRepositoryCustom;
import com.dongsoop.dongsoop.recruitment.tutoring.repository.TutoringBoardRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TutoringApplyServiceImpl implements TutoringApplyService {

    private final MemberService memberService;

    private final TutoringApplyRepository tutoringApplyRepository;

    private final TutoringBoardRepository tutoringBoardRepository;

    private final TutoringApplyRepositoryCustom tutoringApplyRepositoryCustom;

    public void apply(ApplyTutoringBoardRequest request) {
        Member member = memberService.getMemberReferenceByContext();
        validateAlreadyApplied(member.getId(), request.boardId());

        TutoringBoard tutoringBoard = tutoringBoardRepository.findById(request.boardId())
                .orElseThrow(() -> new TutoringBoardNotFound(request.boardId()));

        validateDepartment(tutoringBoard, member);

        TutoringApplyKey key = new TutoringApplyKey(tutoringBoard, member);
        TutoringApply tutoringApply = TutoringApply.builder()
                .id(key)
                .build();

        tutoringApplyRepository.save(tutoringApply);
    }

    private void validateAlreadyApplied(Long memberId, Long boardId) {
        boolean isAlreadyApplied = tutoringApplyRepositoryCustom.existsByBoardIdAndMemberId(boardId, memberId);
        if (isAlreadyApplied) {
            throw new TutoringRecruitmentAlreadyAppliedException(memberId, boardId);
        }
    }

    private void validateDepartment(TutoringBoard tutoringBoard, Member member) {
        Department requesterDepartment = member.getDepartment();

        if (!tutoringBoard.isSameDepartment(requesterDepartment)) {
            Department boardDepartment = tutoringBoard.getDepartment();
            throw new TutoringBoardDepartmentMismatchException(boardDepartment.getId(), requesterDepartment.getId());
        }
    }

    @Override
    @Transactional
    public void updateStatus(Long boardId, UpdateApplyStatusRequest request) {
        Long boardOwnerId = memberService.getMemberIdByAuthentication();
        if (!tutoringBoardRepository.existsByIdAndAuthorId(boardId, boardOwnerId)) {
            throw new TutoringBoardNotFound(boardId, boardOwnerId);
        }

        if (request.compareStatus(RecruitmentApplyStatus.APPLY)) {
            return;
        }

        tutoringApplyRepositoryCustom.updateApplyStatus(request.applierId(), boardId, request.status());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecruitmentApplyOverview> getRecruitmentApplyOverview(Long boardId) {
        Long requesterId = memberService.getMemberIdByAuthentication();

        if (!tutoringBoardRepository.existsByIdAndAuthorId(boardId, requesterId)) {
            throw new TutoringBoardNotFound(boardId, requesterId);
        }

        return tutoringApplyRepository.findApplyOverviewByBoardId(boardId, requesterId);
    }

    @Override
    public ApplyDetails getRecruitmentApplyDetails(Long boardId, Long applierId) {
        Long authorId = memberService.getMemberIdByAuthentication();

        // 게시물 주인이거나 지원자가 아닐 경우 확인할 수 없다.
        if (!tutoringBoardRepository.existsByIdAndAuthorId(boardId, authorId)
                && !tutoringApplyRepositoryCustom.existsByBoardIdAndMemberId(boardId, authorId)) {
            throw new TutoringBoardNotFound(boardId);
        }

        return tutoringApplyRepositoryCustom.findApplyDetailsByBoardIdAndApplierId(boardId, applierId)
                .orElseThrow(() -> new TutoringApplyNotFoundException(boardId, applierId));
    }
}
