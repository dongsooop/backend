package com.dongsoop.dongsoop.marketplace.service;

import com.dongsoop.dongsoop.marketplace.dto.ContactMarketplaceRequest;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceContact;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceContact.MarketplaceContactId;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketplaceContactMapper {

    private final MemberService memberService;

    public MarketplaceContact toEntity(ContactMarketplaceRequest request) {
        Member applicant = memberService.getMemberReferenceByContext();

        MarketplaceContactId id = new MarketplaceContactId(request.boardId(), applicant);

        return MarketplaceContact.builder()
                .id(id)
                .build();
    }
}
