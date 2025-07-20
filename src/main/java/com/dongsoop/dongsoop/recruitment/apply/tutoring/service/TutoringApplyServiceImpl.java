package com.dongsoop.dongsoop.recruitment.apply.tutoring.service;

import com.dongsoop.dongsoop.chat.service.ChatService;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.apply.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.apply.dto.RecruitmentApplyOverview;
import com.dongsoop.dongsoop.recruitment.apply.dto.UpdateApplyStatusRequest;
import com.dongsoop.dongsoop.recruitment.apply.entity.RecruitmentApplyStatus;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.dto.ApplyTutoringBoardRequest;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.entity.TutoringApply;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.entity.TutoringApply.TutoringApplyKey;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.exception.TutoringApplyNotFoundException;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.exception.TutoringOwnerCannotApplyException;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.exception.TutoringRecruitmentAlreadyAppliedException;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.repository.TutoringApplyRepository;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.repository.TutoringApplyRepositoryCustom;
import com.dongsoop.dongsoop.recruitment.board.tutoring.entity.TutoringBoard;
import com.dongsoop.dongsoop.recruitment.board.tutoring.exception.TutoringBoardDepartmentMismatchException;
import com.dongsoop.dongsoop.recruitment.board.tutoring.exception.TutoringBoardNotFound;
import com.dongsoop.dongsoop.recruitment.board.tutoring.repository.TutoringBoardRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TutoringApplyServiceImpl implements TutoringApplyService {

    private final MemberService memberService;

    private final TutoringApplyRepository tutoringApplyRepository;

    private final TutoringBoardRepository tutoringBoardRepository;

    private final TutoringApplyRepositoryCustom tutoringApplyRepositoryCustom;

    private final ChatService chatService;

    public void apply(ApplyTutoringBoardRequest request) {
        Member member = memberService.getMemberReferenceByContext();
        validateAlreadyApplied(member.getId(), request.boardId());

        TutoringBoard tutoringBoard = tutoringBoardRepository.findById(request.boardId())
                .orElseThrow(() -> new TutoringBoardNotFound(request.boardId()));

        if (tutoringBoard.isAuthor(member)) {
            throw new TutoringOwnerCannotApplyException(request.boardId());
        }

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

        if (request.compareStatus(RecruitmentApplyStatus.PASS)) {
            inviteToGroupChat(boardId, request.applierId(), boardOwnerId);
        }
    }

    private void inviteToGroupChat(Long boardId, Long applierId, Long authorId) {
        TutoringBoard tutoringBoard = tutoringBoardRepository.findById(boardId)
                .orElseThrow(() -> new TutoringBoardNotFound(boardId));

        String chatRoomId = tutoringBoard.getRoomId();
        if (chatRoomId == null) {
            log.warn("튜터링 게시판 {}에 채팅방이 연결되지 않음", boardId);
            return;
        }

        chatService.inviteUserToGroupChat(chatRoomId, authorId, applierId);
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
        Long requester = memberService.getMemberIdByAuthentication();

        // 게시물 주인이 아닌 경우 예외
        if (!tutoringBoardRepository.existsByIdAndAuthorId(boardId, requester)) {
            throw new TutoringBoardNotFound(boardId);
        }

        return tutoringApplyRepositoryCustom.findApplyDetailsByBoardIdAndApplierId(boardId, applierId)
                .orElseThrow(() -> new TutoringApplyNotFoundException(boardId, applierId));
    }

    @Override
    public ApplyDetails getRecruitmentApplyDetails(Long boardId) {
        Long requester = memberService.getMemberIdByAuthentication();

        return tutoringApplyRepositoryCustom.findApplyDetailsByBoardIdAndApplierId(boardId, requester)
                .orElseThrow(() -> new TutoringApplyNotFoundException(boardId, requester));
    }
}
