package com.dongsoop.dongsoop.mypage.service;

import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepositoryCustom;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.mypage.dto.ApplyRecruitment;
import com.dongsoop.dongsoop.mypage.dto.OpenedMarketplace;
import com.dongsoop.dongsoop.mypage.dto.OpenedRecruitment;
import com.dongsoop.dongsoop.recruitment.Repository.RecruitmentRepository;
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

    private final MarketplaceBoardRepositoryCustom marketplaceBoardRepositoryCustom;

    private final RecruitmentRepository recruitmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ApplyRecruitment> getApplyRecruitmentsByMemberId(Pageable pageable) {
        Long memberId = memberService.getMemberIdByAuthentication();
        return recruitmentRepository.findApplyRecruitmentsByMemberId(memberId, pageable);
    }

    private int sortedByPageable(OpenedRecruitment compare1, OpenedRecruitment compare2) {
        return compare2.createdAt().compareTo(compare1.createdAt());
    }

    public List<OpenedRecruitment> getOpenedRecruitmentsByMemberId(Pageable pageable) {
        Long memberId = memberService.getMemberIdByAuthentication();

        List<OpenedRecruitment> projectOpenedList = projectBoardRepositoryCustom.findOpenedRecruitmentsByMemberId(
                memberId,
                pageable);
        List<OpenedRecruitment> studyOpenedList = studyBoardRepositoryCustom.findOpenedRecruitmentsByMemberId(memberId,
                pageable);
        List<OpenedRecruitment> tutoringOpenedList = tutoringBoardRepositoryCustom.findOpenedRecruitmentsByMemberId(
                memberId, pageable);

        Stream<OpenedRecruitment> concat = Stream.concat(studyOpenedList.stream(), projectOpenedList.stream());
        return Stream.concat(concat, tutoringOpenedList.stream())
                .sorted(this::sortedByPageable)
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList();
    }

    @Override
    public List<OpenedMarketplace> getOpenedMarketplacesByMemberId(Pageable pageable) {
        Long memberId = memberService.getMemberIdByAuthentication();
        return marketplaceBoardRepositoryCustom.findOpenedMarketplaceByAuthorIdAndPage(memberId, pageable);
    }
}
