package com.dongsoop.dongsoop.marketplace.service;

import com.dongsoop.dongsoop.exception.domain.marketplace.MarketplaceAlreadyAppliedException;
import com.dongsoop.dongsoop.marketplace.dto.ApplyMarketplaceRequest;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceApply;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceApplyRepository;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceApplyRepositoryCustom;
import com.dongsoop.dongsoop.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarketplaceApplyServiceImpl implements MarketplaceApplyService {

    private final MarketplaceApplyRepository marketplaceApplyRepository;

    private final MarketplaceApplyRepositoryCustom marketplaceApplyRepositoryCustom;

    private final MarketplaceApplyMapper marketplaceApplyMapper;

    private final MemberService memberService;

    public void apply(ApplyMarketplaceRequest request) {
        Long memberId = memberService.getMemberIdByAuthentication();
        boolean isAlreadyApplied = marketplaceApplyRepositoryCustom.existsByBoardIdAndMemberId(request.boardId(),
                memberId);

        if (isAlreadyApplied) {
            throw new MarketplaceAlreadyAppliedException(memberId, request.boardId());
        }

        MarketplaceApply marketplaceApply = marketplaceApplyMapper.toEntity(request);
        marketplaceApplyRepository.save(marketplaceApply);
    }
}
