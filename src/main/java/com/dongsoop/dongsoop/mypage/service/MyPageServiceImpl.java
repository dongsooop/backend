package com.dongsoop.dongsoop.mypage.service;

import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepositoryCustom;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.mypage.dto.ApplyRecruitment;
import com.dongsoop.dongsoop.mypage.dto.OpenedMarketplace;
import com.dongsoop.dongsoop.mypage.dto.OpenedRecruitment;
import com.dongsoop.dongsoop.mypage.dto.OpenedRecruitmentResponse;
import com.dongsoop.dongsoop.recruitment.repository.RecruitmentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPageServiceImpl implements MyPageService {

    private final MemberService memberService;

    private final MarketplaceBoardRepositoryCustom marketplaceBoardRepositoryCustom;

    private final RecruitmentRepository recruitmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ApplyRecruitment> getApplyRecruitmentsByMemberId(Pageable pageable) {
        Long memberId = memberService.getMemberIdByAuthentication();
        return recruitmentRepository.findApplyRecruitmentsByMemberId(memberId, pageable);
    }

    @Override
    public List<OpenedRecruitmentResponse> getOpenedRecruitmentsByMemberId(Pageable pageable) {
        Long memberId = memberService.getMemberIdByAuthentication();
        List<OpenedRecruitment> openedRecruitment = recruitmentRepository.findOpenedRecruitmentsByMemberId(memberId,
                pageable);

        return openedRecruitment.stream()
                .map(v -> new OpenedRecruitmentResponse(v))
                .toList();
    }

    @Override
    public List<OpenedMarketplace> getOpenedMarketplacesByMemberId(Pageable pageable) {
        Long memberId = memberService.getMemberIdByAuthentication();
        return marketplaceBoardRepositoryCustom.findOpenedMarketplaceByAuthorIdAndPage(memberId, pageable);
    }
}
