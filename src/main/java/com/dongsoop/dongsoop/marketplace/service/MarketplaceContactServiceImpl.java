package com.dongsoop.dongsoop.marketplace.service;

import com.dongsoop.dongsoop.exception.domain.marketplace.MarketplaceAlreadyAppliedException;
import com.dongsoop.dongsoop.marketplace.dto.ContactMarketplaceRequest;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceContact;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceContactRepository;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceContactRepositoryCustom;
import com.dongsoop.dongsoop.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarketplaceContactServiceImpl implements MarketplaceContactService {

    private final MarketplaceContactRepository marketplaceContactRepository;

    private final MarketplaceContactRepositoryCustom marketplaceContactRepositoryCustom;

    private final MarketplaceContactMapper marketplaceContactMapper;

    private final MemberService memberService;

    public void contact(ContactMarketplaceRequest request) {
        Long memberId = memberService.getMemberIdByAuthentication();
        boolean isAlreadyApplied = marketplaceContactRepositoryCustom.existsByBoardIdAndMemberId(request.boardId(),
                memberId);

        if (isAlreadyApplied) {
            throw new MarketplaceAlreadyAppliedException(memberId, request.boardId());
        }

        MarketplaceContact marketplaceContact = marketplaceContactMapper.toEntity(request);
        marketplaceContactRepository.save(marketplaceContact);
    }
}
