package com.dongsoop.dongsoop.recruitment.board.tutoring.entity;

import com.dongsoop.dongsoop.board.RecruitmentBoard;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

@Entity
@SuperBuilder
@NoArgsConstructor
@SequenceGenerator(name = "tutoring_board_sequence_generator")
@SQLRestriction("is_deleted = false")
public class TutoringBoard extends RecruitmentBoard {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tutoring_board_sequence_generator")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    public boolean equalsId(TutoringBoard that) {
        return Objects.equals(this.id, that.id);
    }

    public boolean isSameDepartment(Department that) {
        DepartmentType thisDepartmentType = this.department.getId();
        DepartmentType thatDepartmentType = that.getId();

        return thisDepartmentType.equals(thatDepartmentType);
    }

    public Department getDepartment() {
        return this.department;
    }
}
