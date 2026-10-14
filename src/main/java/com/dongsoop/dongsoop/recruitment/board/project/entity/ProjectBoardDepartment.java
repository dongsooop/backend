package com.dongsoop.dongsoop.recruitment.board.project.entity;

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
@IdClass(ProjectBoardDepartment.ProjectBoardDepartmentId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProjectBoardDepartment {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_board_id")
    private ProjectBoard projectBoard;

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
    public static class ProjectBoardDepartmentId implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long projectBoard;

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
            ProjectBoardDepartmentId that = (ProjectBoardDepartmentId) o;
            return Objects.equals(projectBoard, that.projectBoard)
                    && Objects.equals(department, that.department);
        }

        @Override
        public int hashCode() {
            return Objects.hash(projectBoard, department);
        }
    }
}
