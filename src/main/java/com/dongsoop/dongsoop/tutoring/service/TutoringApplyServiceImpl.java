package com.dongsoop.dongsoop.tutoring.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.tutoring.entity.TutoringApplication;
import com.dongsoop.dongsoop.tutoring.entity.TutoringApplication.TutoringApplicationKey;
import com.dongsoop.dongsoop.tutoring.entity.TutoringBoard;
import com.dongsoop.dongsoop.tutoring.repository.TutoringApplicationRepository;
import com.dongsoop.dongsoop.tutoring.repository.TutoringBoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TutoringApplyServiceImpl implements TutoringApplyService{

    private final MemberService memberService;

    private final TutoringApplicationRepository tutoringApplicationRepository;

    private final TutoringBoardRepository tutoringBoardRepository;

    public void apply(Long tutoringBoardId) {
        Member member = memberService.getMemberReferenceByContext();
        TutoringBoard referenceById = tutoringBoardRepository.getReferenceById(tutoringBoardId);

        TutoringApplicationKey key = new TutoringApplicationKey(referenceById, member);

        TutoringApplication tutoringApplication = TutoringApplication.builder()
                .id(key)
                .build();

        tutoringApplicationRepository.save(tutoringApplication);
    }
}
