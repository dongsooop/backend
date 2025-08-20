package com.dongsoop.dongsoop.recruitment.apply.study.service;

import com.dongsoop.dongsoop.chat.service.ChatService;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.apply.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.apply.dto.RecruitmentApplyOverview;
import com.dongsoop.dongsoop.recruitment.apply.dto.UpdateApplyStatusRequest;
import com.dongsoop.dongsoop.recruitment.apply.entity.RecruitmentApplyStatus;
import com.dongsoop.dongsoop.recruitment.apply.study.dto.ApplyStudyBoardRequest;
import com.dongsoop.dongsoop.recruitment.apply.study.entity.StudyApply;
import com.dongsoop.dongsoop.recruitment.apply.study.entity.StudyApply.StudyApplyKey;
import com.dongsoop.dongsoop.recruitment.apply.study.exception.StudyApplyNotFoundException;
import com.dongsoop.dongsoop.recruitment.apply.study.exception.StudyOwnerCannotApplyException;
import com.dongsoop.dongsoop.recruitment.apply.study.exception.StudyRecruitmentAlreadyAppliedException;
import com.dongsoop.dongsoop.recruitment.apply.study.notification.StudyApplyNotification;
import com.dongsoop.dongsoop.recruitment.apply.study.repository.StudyApplyRepository;
import com.dongsoop.dongsoop.recruitment.apply.study.repository.StudyApplyRepositoryCustom;
import com.dongsoop.dongsoop.recruitment.board.study.entity.StudyBoard;
import com.dongsoop.dongsoop.recruitment.board.study.entity.StudyBoardDepartment;
import com.dongsoop.dongsoop.recruitment.board.study.exception.StudyBoardDepartmentMismatchException;
import com.dongsoop.dongsoop.recruitment.board.study.exception.StudyBoardDepartmentNotAssignedException;
import com.dongsoop.dongsoop.recruitment.board.study.exception.StudyBoardNotFound;
import com.dongsoop.dongsoop.recruitment.board.study.repository.StudyBoardDepartmentRepository;
import com.dongsoop.dongsoop.recruitment.board.study.repository.StudyBoardRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyApplyServiceImpl implements StudyApplyService {

    private final MemberService memberService;
    private final StudyApplyRepository studyApplyRepository;
    private final StudyBoardRepository studyBoardRepository;
    private final StudyBoardDepartmentRepository studyBoardDepartmentRepository;
    private final StudyApplyRepositoryCustom studyApplyRepositoryCustom;
    private final ChatService chatService;
    private final StudyApplyNotification studyApplyNotification;

    public void apply(ApplyStudyBoardRequest request) {
        Member member = memberService.getMemberReferenceByContext();
        validateAlreadyApplied(member.getId(), request.boardId());

        StudyBoard studyBoard = studyBoardRepository.findById(request.boardId())
                .orElseThrow(() -> new StudyBoardNotFound(request.boardId()));

        if (studyBoard.isAuthor(member)) {
            throw new StudyOwnerCannotApplyException(request.boardId());
        }

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

        Long authorId = studyBoard.getAuthor().getId();
        studyApplyNotification.sendApplyNotification(studyBoard.getId(), studyBoard.getTitle(), authorId);
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
        Long boardOwnerId = memberService.getMemberIdByAuthentication();
        if (!studyBoardRepository.existsByIdAndAuthorId(boardId, boardOwnerId)) {
            throw new StudyBoardNotFound(boardId, boardOwnerId);
        }

        if (request.compareStatus(RecruitmentApplyStatus.APPLY)) {
            return;
        }

        studyApplyRepositoryCustom.updateApplyStatus(request.applierId(), boardId, request.status());

        if (request.compareStatus(RecruitmentApplyStatus.PASS)) {
            inviteToGroupChat(boardId, request.applierId(), boardOwnerId);
        }
    }

    private void inviteToGroupChat(Long boardId, Long applierId, Long authorId) {
        StudyBoard studyBoard = studyBoardRepository.findById(boardId)
                .orElseThrow(() -> new StudyBoardNotFound(boardId));

        if (!studyBoard.hasChatRoom()) {
            log.warn("게시판 {}에 채팅방이 연결되지 않음", boardId);
            return;
        }

        chatService.inviteUserToGroupChat(studyBoard.getRoomId(), authorId, applierId);
    }


    @Override
    @Transactional(readOnly = true)
    public List<RecruitmentApplyOverview> getRecruitmentApplyOverview(Long boardId) {
        Long requesterId = memberService.getMemberIdByAuthentication();

        if (!studyBoardRepository.existsByIdAndAuthorId(boardId, requesterId)) {
            throw new StudyBoardNotFound(boardId, requesterId);
        }

        return studyApplyRepository.findApplyOverviewByBoardId(boardId, requesterId);
    }

    @Override
    public ApplyDetails getRecruitmentApplyDetails(Long boardId, Long applierId) {
        Long requester = memberService.getMemberIdByAuthentication();

        // 게시물 주인이 아닌 경우 예외
        if (!studyBoardRepository.existsByIdAndAuthorId(boardId, requester)) {
            throw new StudyBoardNotFound(boardId);
        }

        return studyApplyRepositoryCustom.findApplyDetailsByBoardIdAndApplierId(boardId, applierId)
                .orElseThrow(() -> new StudyApplyNotFoundException(boardId, applierId));
    }

    @Override
    public ApplyDetails getRecruitmentApplyDetails(Long boardId) {
        Long requester = memberService.getMemberIdByAuthentication();

        return studyApplyRepositoryCustom.findApplyDetailsByBoardIdAndApplierId(boardId, requester)
                .orElseThrow(() -> new StudyApplyNotFoundException(boardId, requester));
    }
}
