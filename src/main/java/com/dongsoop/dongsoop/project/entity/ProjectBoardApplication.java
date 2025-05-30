package com.dongsoop.dongsoop.project.entity;

import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@SuperBuilder
@NoArgsConstructor
public class ProjectBoardApplication {

    @EmbeddedId
    private ProjectBoardApplicationKey id;

    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectBoardApplicationKey {

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(nullable = false, name = "project_board_id", updatable = false)
        private ProjectBoard projectBoard;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(nullable = false, name = "member_id", updatable = false)
        private Member member;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ProjectBoardApplicationKey that = (ProjectBoardApplicationKey) o;
            return this.projectBoard.equalsId(that.projectBoard)
                    && Objects.equals(this.member.getId(), that.member.getId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(projectBoard.getId(), member.getId());
        }
    }
}
