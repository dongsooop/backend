package com.dongsoop.dongsoop.marketplace.entity;

import com.dongsoop.dongsoop.common.BaseEntity;
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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@IdClass(MarketplaceImage.MarketplaceImageId.class)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
@SQLDelete(sql = "UPDATE marketplace_image SET is_deleted = true WHERE marketplace_board_id = ? AND url = ?")
public class MarketplaceImage extends BaseEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marketplace_board_id", nullable = false, updatable = false)
    MarketplaceBoard marketplaceBoard;

    @Id
    @Column(name = "url", nullable = false, updatable = false)
    String url;

    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketplaceImageId implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        Long marketplaceBoard;

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
            return Objects.hash(marketplaceBoard, url);
        }
    }
}
