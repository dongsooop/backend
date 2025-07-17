package com.dongsoop.dongsoop.board;

import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Objects;

@MappedSuperclass
@SuperBuilder
@NoArgsConstructor
public abstract class Board extends BaseEntity {

    @NotBlank
    @Column(name = "title", length = 20, nullable = false)
    protected String title;

    @NotBlank
    @Column(name = "content", length = 500, nullable = false)
    protected String content;

    @Getter
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author", nullable = false, updatable = false)
    protected Member author;

    public boolean isAuthor(Member author) {
        return Objects.equals(this.author, author);
    }
}
