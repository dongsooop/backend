package com.dongsoop.dongsoop.board;

import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@SuperBuilder
@NoArgsConstructor
public abstract class Board extends BaseEntity {

    @NotBlank
    @Column(name = "title", length = 20, nullable = false)
    private String title;

    @NotBlank
    @Column(name = "content", length = 500, nullable = false)
    private String content;

    @Embedded
    @NotNull
    private BoardDate boardDate;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author", nullable = false, updatable = false)
    private Member author;

    @PrePersist
    public void onPrePersist() {
        this.boardDate = new BoardDate();
    }

    @PreUpdate
    public void onPreUpdate() {
        this.boardDate.updateDate();
    }
}
