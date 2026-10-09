package com.dongsoop.dongsoop.marketplace.entity;

import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@IdClass(MarketplaceContact.MarketplaceContactId.class)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketplaceContact extends BaseEntity {

    @Id
    @Column(name = "marketplace_id", nullable = false, updatable = false)
    private Long marketplaceId;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant", nullable = false, updatable = false)
    private Member applicant;

    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class MarketplaceContactId implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long marketplaceId;

        private Long applicant;

        @Override
        public boolean equals(Object that) {
            if (this == that) {
                return true;
            }

            if (that == null || getClass() != that.getClass()) {
                return false;
            }

            MarketplaceContactId thatId = (MarketplaceContactId) that;
            return marketplaceId.equals(thatId.marketplaceId) && applicant.equals(thatId.applicant);
        }

        @Override
        public int hashCode() {
            return Objects.hash(marketplaceId, applicant);
        }
    }
}
