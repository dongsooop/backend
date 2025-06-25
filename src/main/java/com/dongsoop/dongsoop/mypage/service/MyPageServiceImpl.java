package com.dongsoop.dongsoop.mypage.service;

import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.mypage.dto.ApplyRecruitment;
import com.dongsoop.dongsoop.recruitment.project.repository.ProjectBoardRepositoryCustom;
import com.dongsoop.dongsoop.recruitment.study.repository.StudyBoardRepositoryCustom;
import com.dongsoop.dongsoop.recruitment.tutoring.repository.TutoringBoardRepositoryCustom;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPageServiceImpl implements MyPageService {

    private final MemberService memberService;

    private final TutoringBoardRepositoryCustom tutoringBoardRepositoryCustom;
    private final StudyBoardRepositoryCustom studyBoardRepositoryCustom;
    private final ProjectBoardRepositoryCustom projectBoardRepositoryCustom;

    @Override
    @Transactional(readOnly = true)
    public List<ApplyRecruitment> getApplyRecruitmentsByMemberId(Pageable pageable) {
        Long memberId = memberService.getMemberIdByAuthentication();

        List<ApplyRecruitment> projectApplyList = projectBoardRepositoryCustom.findApplyRecruitmentsByMemberId(memberId,
                pageable);
        List<ApplyRecruitment> studyApplyList = studyBoardRepositoryCustom.findApplyRecruitmentsByMemberId(memberId,
                pageable);
        List<ApplyRecruitment> tutoringApplyList = tutoringBoardRepositoryCustom.findApplyRecruitmentsByMemberId(
                memberId, pageable);

        Stream<ApplyRecruitment> concat = Stream.concat(studyApplyList.stream(), projectApplyList.stream());
        return Stream.concat(concat, tutoringApplyList.stream())
                .sorted(this::sortedByPageable)
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList();
    }

    private int sortedByPageable(ApplyRecruitment compare1, ApplyRecruitment compare2) {
        return compare2.createdAt().compareTo(compare1.createdAt());
    }
}
