package com.dongsoop.dongsoop.study.entity;

import com.dongsoop.dongsoop.department.entity.Department;
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

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StudyBoardDepartment {

    @EmbeddedId
    private StudyBoardDepartmentId id;

    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudyBoardDepartmentId {

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "study_board_id")
        private StudyBoard studyBoard;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "department_id")
        private Department department;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            StudyBoardDepartmentId that = (StudyBoardDepartmentId) o;
            return this.studyBoard.equalsId(that.studyBoard) && this.department.equalsId(that.department);
        }

        @Override
        public int hashCode() {
            return Objects.hash(studyBoard.getId(), department.getId());
        }
    }
}
