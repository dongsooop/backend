package com.dongsoop.dongsoop.marketplace.repository;

import com.dongsoop.dongsoop.marketplace.entity.MarketplaceBoard;
import com.dongsoop.dongsoop.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketplaceBoardRepository extends JpaRepository<MarketplaceBoard, Long> {

    boolean existsByIdAndAuthor(Long id, Member author);
}
