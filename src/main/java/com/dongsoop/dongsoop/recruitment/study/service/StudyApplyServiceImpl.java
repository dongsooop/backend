package com.dongsoop.dongsoop.recruitment.study.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.study.dto.ApplyStudyBoardRequest;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoard;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoardApply;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoardApply.StudyBoardApplyKey;
import com.dongsoop.dongsoop.recruitment.study.repository.StudyApplyRepository;
import com.dongsoop.dongsoop.recruitment.study.repository.StudyBoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudyApplyServiceImpl implements StudyApplyService {

    private final MemberService memberService;

    private final StudyApplyRepository studyApplyRepository;

    private final StudyBoardRepository studyBoardRepository;

    public void apply(ApplyStudyBoardRequest request) {
        Member member = memberService.getMemberReferenceByContext();
        StudyBoard referenceById = studyBoardRepository.getReferenceById(request.boardId());

        StudyBoardApplyKey key = new StudyBoardApplyKey(referenceById, member);

        StudyBoardApply studyApplication = StudyBoardApply.builder()
                .id(key)
                .build();

        studyApplyRepository.save(studyApplication);
    }
}
