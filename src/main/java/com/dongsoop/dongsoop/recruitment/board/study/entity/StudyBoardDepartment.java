package com.dongsoop.dongsoop.recruitment.board.study.entity;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@IdClass(StudyBoardDepartment.StudyBoardDepartmentId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StudyBoardDepartment {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_board_id")
    private StudyBoard studyBoard;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    public boolean isSameDepartmentType(DepartmentType that) {
        DepartmentType thisDepartmentType = this.department.getId();

        return thisDepartmentType.equals(that);
    }

    public Department getDepartment() {
        return this.department;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudyBoardDepartmentId implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long studyBoard;

        @Enumerated(EnumType.STRING)
        private DepartmentType department;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            StudyBoardDepartmentId that = (StudyBoardDepartmentId) o;
            return Objects.equals(studyBoard, that.studyBoard)
                    && Objects.equals(department, that.department);
        }

        @Override
        public int hashCode() {
            return Objects.hash(studyBoard, department);
        }
    }
}
