package com.dongsoop.dongsoop.marketplace.entity;

import com.dongsoop.dongsoop.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

@Entity
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class MarketplaceImage extends BaseEntity {

    @EmbeddedId
    private MarketplaceImageId id;

    @Embeddable
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class MarketplaceImageId {

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "marketplace_board_id", nullable = false, updatable = false)
        MarketplaceBoard marketplaceBoard;

        @Column(name = "url", nullable = false, updatable = false)
        String url;

        @Override
        public boolean equals(Object that) {
            if (this == that) {
                return true;
            }
            if (that == null || getClass() != that.getClass()) {
                return false;
            }
            MarketplaceImageId thatId = (MarketplaceImageId) that;
            return marketplaceBoard.equals(thatId.marketplaceBoard) && url.equals(thatId.url);
        }

        @Override
        public int hashCode() {
            return Objects.hash(marketplaceBoard.getId(), url);
        }
    }
}
