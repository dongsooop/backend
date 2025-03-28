package com.dongsoop.dongsoop.notice.entity;

import com.dongsoop.dongsoop.department.DepartmentType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentNotice {

    @Id
    private DepartmentNoticeKey id;

    @Embeddable
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DepartmentNoticeKey {
        @Enumerated(EnumType.STRING)
        private DepartmentType type;

        @ManyToOne(cascade = CascadeType.PERSIST)
        @JoinColumn
        private Notice notice;

        // JPA 엔티티 비교 및 캐싱 시 사용된다
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DepartmentNoticeKey that = (DepartmentNoticeKey) o;
            return type == that.type && Objects.equals(notice, that.notice);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, notice);
        }
    }
}
