package com.dongsoop.dongsoop.recruitment.tutoring.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.TutoringApply;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.TutoringApply.TutoringApplyKey;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.TutoringBoard;
import com.dongsoop.dongsoop.recruitment.tutoring.repository.TutoringApplicationRepository;
import com.dongsoop.dongsoop.recruitment.tutoring.repository.TutoringBoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TutoringApplyServiceImpl implements TutoringApplyService {

    private final MemberService memberService;

    private final TutoringApplicationRepository tutoringApplicationRepository;

    private final TutoringBoardRepository tutoringBoardRepository;

    public void apply(Long tutoringBoardId) {
        Member member = memberService.getMemberReferenceByContext();
        TutoringBoard referenceById = tutoringBoardRepository.getReferenceById(tutoringBoardId);

        TutoringApplyKey key = new TutoringApplyKey(referenceById, member);

        TutoringApply tutoringApplication = TutoringApply.builder()
                .id(key)
                .build();

        tutoringApplicationRepository.save(tutoringApplication);
    }
}
