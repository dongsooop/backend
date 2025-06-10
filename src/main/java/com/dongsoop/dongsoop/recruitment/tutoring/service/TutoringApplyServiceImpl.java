package com.dongsoop.dongsoop.recruitment.tutoring.service;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.exception.domain.tutoring.TutoringBoardDepartmentMismatchException;
import com.dongsoop.dongsoop.exception.domain.tutoring.TutoringBoardNotFound;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.ApplyTutoringBoardRequest;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.TutoringApply;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.TutoringApply.TutoringApplyKey;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.TutoringBoard;
import com.dongsoop.dongsoop.recruitment.tutoring.repository.TutoringApplyRepository;
import com.dongsoop.dongsoop.recruitment.tutoring.repository.TutoringBoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TutoringApplyServiceImpl implements TutoringApplyService {

    private final MemberService memberService;

    private final TutoringApplyRepository tutoringApplyRepository;

    private final TutoringBoardRepository tutoringBoardRepository;

    public void apply(ApplyTutoringBoardRequest request) {
        Member member = memberService.getMemberReferenceByContext();
        TutoringBoard tutoringBoard = tutoringBoardRepository.findById(request.boardId())
                .orElseThrow(() -> new TutoringBoardNotFound(request.boardId()));

        validateDepartment(tutoringBoard, member);

        TutoringApplyKey key = new TutoringApplyKey(tutoringBoard, member);
        TutoringApply tutoringApply = TutoringApply.builder()
                .id(key)
                .build();

        tutoringApplyRepository.save(tutoringApply);
    }

    private void validateDepartment(TutoringBoard tutoringBoard, Member member) {
        Department requesterDepartment = member.getDepartment();

        if (!tutoringBoard.isSameDepartment(requesterDepartment)) {
            Department boardDepartment = tutoringBoard.getDepartment();
            throw new TutoringBoardDepartmentMismatchException(boardDepartment.getId(), requesterDepartment.getId());
        }
    }
}
