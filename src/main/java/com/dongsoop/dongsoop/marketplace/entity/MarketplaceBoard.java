package com.dongsoop.dongsoop.marketplace.entity;

import com.dongsoop.dongsoop.board.Board;
import com.dongsoop.dongsoop.exception.domain.marketplace.MarketplaceBoardAlreadyClosedException;
import com.dongsoop.dongsoop.marketplace.dto.UpdateMarketplaceBoardRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.StringUtils;

@Entity
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(name = "marketplace_board_sequence_generator")
@SQLRestriction("is_deleted = false")
@SQLDelete(sql = "UPDATE marketplace_board SET is_deleted = true WHERE id = ?")
public class MarketplaceBoard extends Board {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "marketplace_board_sequence_generator")
    private Long id;

    @Column(name = "price", nullable = false)
    private Long price;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private MarketplaceBoardStatus status = MarketplaceBoardStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MarketplaceType type;

    public void update(UpdateMarketplaceBoardRequest request) {
        if (request.price() != null) {
            this.price = request.price();
        }
        if (request.type() != null) {
            this.type = request.type();
        }
        if (StringUtils.hasText(request.title())) {
            super.title = request.title();
        }
        if (StringUtils.hasText(request.content())) {
            super.content = request.content();
        }
    }

    public void close() {
        if (this.status != MarketplaceBoardStatus.OPEN) {
            throw new MarketplaceBoardAlreadyClosedException(this.id);
        }

        this.status = MarketplaceBoardStatus.CLOSED;
    }
}
