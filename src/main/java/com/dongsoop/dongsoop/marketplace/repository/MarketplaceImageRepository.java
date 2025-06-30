package com.dongsoop.dongsoop.marketplace.repository;

import com.dongsoop.dongsoop.marketplace.entity.MarketplaceImage;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceImage.MarketplaceImageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketplaceImageRepository extends JpaRepository<MarketplaceImage, MarketplaceImageId> {

    @Query("SELECT COUNT(mi) FROM MarketplaceImage mi WHERE mi.id.marketplaceBoard.id = :boardId")
    Integer countByMarketplaceBoardId(@Param("boardId") Long boardId);

    @Modifying
    @Query("UPDATE MarketplaceImage mi SET mi.isDeleted = true WHERE mi.id.marketplaceBoard.id = :boardId")
    void deleteByMarketplaceBoardId(@Param("boardId") Long boardId);
}
