package com.dongsoop.dongsoop.marketplace.repository;

import com.dongsoop.dongsoop.marketplace.entity.MarketplaceApply;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceApply.MarketplaceApplyId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketplaceApplyRepository extends JpaRepository<MarketplaceApply, MarketplaceApplyId> {
}
