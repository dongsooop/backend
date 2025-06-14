package com.dongsoop.dongsoop.marketplace.repository;

import com.dongsoop.dongsoop.marketplace.entity.MarketplaceImage;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceImage.MarketplaceImageId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketplaceImageRepository extends JpaRepository<MarketplaceImage, MarketplaceImageId> {
}
