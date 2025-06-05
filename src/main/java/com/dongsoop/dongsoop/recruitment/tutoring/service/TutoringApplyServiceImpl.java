package com.dongsoop.dongsoop.recruitment.tutoring.service;

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
        TutoringBoard referenceById = tutoringBoardRepository.getReferenceById(request.boardId());

        TutoringApplyKey key = new TutoringApplyKey(referenceById, member);

        TutoringApply tutoringApply = TutoringApply.builder()
                .id(key)
                .build();

        tutoringApplyRepository.save(tutoringApply);
    }
}
