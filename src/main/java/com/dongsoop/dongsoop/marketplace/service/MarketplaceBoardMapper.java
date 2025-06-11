package com.dongsoop.dongsoop.marketplace.service;

import com.dongsoop.dongsoop.marketplace.dto.CreateMarketplaceBoardRequest;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceBoard;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketplaceBoardMapper {

    private final MemberService memberService;

    public MarketplaceBoard toEntity(CreateMarketplaceBoardRequest request) {
        Member author = memberService.getMemberReferenceByContext();

        return MarketplaceBoard.builder()
                .title(request.title())
                .content(request.content())
                .price(request.price())
                .author(author)
                .build();
    }
}
