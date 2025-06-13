package com.dongsoop.dongsoop.marketplace.service;

import com.dongsoop.dongsoop.marketplace.dto.ApplyMarketplaceRequest;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceApply;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceApply.MarketplaceApplyId;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketplaceApplyMapper {

    private final MemberService memberService;

    public MarketplaceApply toEntity(ApplyMarketplaceRequest request) {
        Member applicant = memberService.getMemberReferenceByContext();

        MarketplaceApplyId id = new MarketplaceApplyId(request.boardId(), applicant);

        return MarketplaceApply.builder()
                .id(id)
                .build();
    }
}
