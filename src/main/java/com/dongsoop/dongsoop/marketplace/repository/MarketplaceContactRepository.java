package com.dongsoop.dongsoop.marketplace.repository;

import com.dongsoop.dongsoop.marketplace.entity.MarketplaceContact;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceContact.MarketplaceContactId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketplaceContactRepository extends JpaRepository<MarketplaceContact, MarketplaceContactId> {
}
