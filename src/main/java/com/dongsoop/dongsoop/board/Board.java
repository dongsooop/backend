package com.dongsoop.dongsoop.board;

import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

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

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author", nullable = false, updatable = false)
    protected Member author;

    public boolean isAuthor(Member author) {
        return Objects.equals(this.author, author);
    }
}
