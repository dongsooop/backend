package com.dongsoop.dongsoop.marketplace.entity;

import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.member.entity.Member;
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

@Entity
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketplaceApply extends BaseEntity {

    @EmbeddedId
    private MarketplaceApplyId id;

    @Embeddable
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class MarketplaceApplyId {

        @Column(name = "marketplace_id", nullable = false, updatable = false)
        private Long marketplaceId;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "applicant", nullable = false, updatable = false)
        private Member applicant;

        @Override
        public boolean equals(Object that) {
            if (this == that) {
                return true;
            }

            if (that == null || getClass() != that.getClass()) {
                return false;
            }

            MarketplaceApplyId thatId = (MarketplaceApplyId) that;
            return marketplaceId.equals(thatId.marketplaceId) && applicant.getId().equals(thatId.applicant.getId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(marketplaceId, applicant.getId());
        }
    }
}
