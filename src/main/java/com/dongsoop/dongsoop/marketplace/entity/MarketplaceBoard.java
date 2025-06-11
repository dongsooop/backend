package com.dongsoop.dongsoop.marketplace.entity;

import com.dongsoop.dongsoop.board.Board;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

@Entity
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(name = "marketplace_board_sequence_generator")
@SQLRestriction("is_deleted = false")
public class MarketplaceBoard extends Board {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "marketplace_board_sequence_generator")
    private Long id;

    @Column(name = "price", nullable = false)
    private Long price;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private MarketplaceBoardStatus status = MarketplaceBoardStatus.SELLING;
}
