package com.dongsoop.dongsoop.marketplace.repository;

public interface MarketplaceContactRepositoryCustom {

    boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);
}
