package com.dongsoop.dongsoop.marketplace.repository;

public interface MarketplaceApplyRepositoryCustom {

    boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);
}
